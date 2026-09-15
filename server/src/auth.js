const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { z } = require('zod');
const crypto = require('crypto');
const { OAuth2Client } = require('google-auth-library');
const { sendVerificationLink, sendPasswordResetCode, isEmailConfigured } = require('./email');
const { JWT_SECRET } = require('./config');

const VERIFICATION_TOKEN_TTL_MS = 24 * 60 * 60 * 1000;

// Recuperacion de contrasena por codigo.
const RESET_CODE_TTL_MS = 15 * 60 * 1000;   // el codigo vive 15 minutos
const RESET_MAX_ATTEMPTS = 5;               // intentos antes de invalidarlo
const RESET_RESEND_COOLDOWN_MS = 60 * 1000; // espera minima entre envios

const routerFactory = (prisma) => {
  const router = express.Router();
  const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || '';
  const googleClient = GOOGLE_CLIENT_ID ? new OAuth2Client(GOOGLE_CLIENT_ID) : null;

  const emailCreds = z.object({ email: z.string().email(), password: z.string().min(6) });
  const registerSchema = z.object({
    email: z.string().email(),
    password: z.string()
      .min(8)
      .regex(/[A-Z]/)
      .regex(/[a-z]/)
      .regex(/[0-9]/),
    name: z.string().min(1)
  });

  function signTokens(userId) {
    const accessToken = jwt.sign({ uid: userId }, JWT_SECRET, { expiresIn: '15m' });
    const refreshToken = jwt.sign({ uid: userId, typ: 'refresh' }, JWT_SECRET, { expiresIn: '30d' });
    return { accessToken, refreshToken };
  }

  const googleSchema = z.object({ idToken: z.string().min(1) });

  const resetSchema = z.object({
    email: z.string().email(),
    code: z.string().regex(/^[0-9]{6}$/),
    password: z.string().min(8).regex(/[A-Z]/).regex(/[a-z]/).regex(/[0-9]/)
  });

  async function sendVerification(prisma, user) {
    if (!isEmailConfigured()) throw new Error('email_provider_not_configured');
    const token = crypto.randomUUID();
    await prisma.user.update({
      where: { id: user.id },
      data: { verificationToken: token, verificationSentAt: new Date() }
    });
    const baseUrl = process.env.APP_BASE_URL || 'http://localhost:3000';
    const link = `${baseUrl}/auth/verify?token=${encodeURIComponent(token)}`;
    await sendVerificationLink(user, link);
  }

  router.post('/register', async (req, res) => {
    const parse = registerSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const { password, name } = parse.data;
    const email = parse.data.email.trim().toLowerCase();
    if (!isEmailConfigured()) return res.status(503).json({ error: 'email_provider_not_configured' });
    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) return res.status(409).json({ error: 'email_in_use' });
    const hash = await bcrypt.hash(password, 10);
    const user = await prisma.user.create({ data: { email, passwordHash: hash, name, emailVerified: false } });
    try {
      await sendVerification(prisma, user);
    } catch (e) {
      console.error('sendVerification error', e);
      return res.status(503).json({ error: 'email_delivery_failed' });
    }
    res.json({ ok: true });
  });

  router.post('/login', async (req, res) => {
    const parse = emailCreds.safeParse(req.body || {});
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const { email, password } = parse.data;
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(401).json({ error: 'invalid_credentials' });
    if (!user.passwordHash) return res.status(401).json({ error: 'invalid_credentials' });
    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ error: 'invalid_credentials' });
    if (!user.emailVerified) return res.status(403).json({ error: 'email_not_verified' });
    const tokens = signTokens(user.id);
    res.json({ user: { id: user.id, email: user.email, name: user.name || null }, ...tokens });
  });

  router.post('/google', async (req, res) => {
    const parse = googleSchema.safeParse(req.body || {});
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    if (!googleClient) return res.status(501).json({ error: 'google_not_configured' });

    // El try se cierra apenas termina la verificacion: antes envolvia tambien la
    // escritura en base, asi que una caida de Postgres se reportaba como
    // 'invalid_token' y culpaba al token de un problema de infraestructura.
    let payload;
    try {
      const ticket = await googleClient.verifyIdToken({
        idToken: parse.data.idToken,
        audience: GOOGLE_CLIENT_ID,
      });
      payload = ticket.getPayload();
    } catch (e) {
      console.error('[auth][google] token verification failed:', e?.message || e);
      return res.status(401).json({ error: 'invalid_token' });
    }

    const email = payload && payload.email;
    const googleSub = payload && payload.sub;
    const name = payload && payload.name;
    if (!email || !googleSub || payload.email_verified === false) {
      return res.status(401).json({ error: 'invalid_token' });
    }

    const normalizedEmail = email.trim().toLowerCase();

    try {
      const user = await prisma.user.upsert({
        where: { email: normalizedEmail },
        update: {
          name: name || undefined,
          googleSub,
          emailVerified: true,
        },
        create: {
          email: normalizedEmail,
          name: name || null,
          passwordHash: null,
          googleSub,
          emailVerified: true,
        },
      });

      const tokens = signTokens(user.id);
      return res.json({ user: { id: user.id, email: user.email, name: user.name || null }, ...tokens });
    } catch (e) {
      // Aqui solo llegan fallos de base de datos: el token ya fue validado.
      console.error('[auth][google] database error:', e?.message || e);
      return res.status(503).json({ error: 'database_unavailable' });
    }
  });

  router.post('/refresh', async (req, res) => {
    const { refreshToken } = req.body || {};
    if (!refreshToken) return res.status(400).json({ error: 'missing_token' });
    let payload;
    try {
      payload = jwt.verify(refreshToken, JWT_SECRET);
      if (payload.typ !== 'refresh') throw new Error('bad_typ');
    } catch {
      return res.status(401).json({ error: 'invalid_token' });
    }
    try {
      // Un cambio de contrasena invalida los refresh emitidos antes. El access
      // token vive 15 minutos, asi que una sesion robada muere como mucho en ese
      // plazo, sin pagar una consulta a la base en cada peticion.
      const user = await prisma.user.findUnique({
        where: { id: payload.uid },
        select: { passwordChangedAt: true }
      });
      if (!user) return res.status(401).json({ error: 'invalid_token' });
      if (user.passwordChangedAt &&
          payload.iat * 1000 < Math.floor(user.passwordChangedAt.getTime() / 1000) * 1000) {
        return res.status(401).json({ error: 'password_changed' });
      }
      return res.json(signTokens(payload.uid));
    } catch (e) {
      console.error('[auth][refresh] database error:', e?.message || e);
      return res.status(503).json({ error: 'database_unavailable' });
    }
  });

  // Resend verification email
  router.post('/send-verification', async (req, res) => {
    const rawEmail = req.body?.email;
    const email = typeof rawEmail === 'string' ? rawEmail.trim().toLowerCase() : rawEmail;
    if (!email || typeof email !== 'string') return res.status(400).json({ error: 'invalid_body' });
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(200).json({ ok: true });
    if (user.emailVerified) return res.status(200).json({ ok: true });
    try {
      await sendVerification(prisma, user);
    } catch (e) {
      console.error('sendVerification error', e);
      return res.status(503).json({ error: 'email_delivery_failed' });
    }
    res.json({ ok: true });
  });

  // --- Recuperacion de contrasena -------------------------------------------
  // Paso 1: pedir el codigo. Responde 200 siempre, exista o no la cuenta: si
  // distinguiera, cualquiera podria averiguar que correos estan registrados.
  router.post('/forgot-password', async (req, res) => {
    const raw = req.body?.email;
    const email = typeof raw === 'string' ? raw.trim().toLowerCase() : null;
    if (!email) return res.status(400).json({ error: 'invalid_body' });
    if (!isEmailConfigured()) return res.status(503).json({ error: 'email_provider_not_configured' });

    try {
      const user = await prisma.user.findUnique({ where: { email } });
      if (!user) return res.json({ ok: true });

      // Freno de reenvio: evita usar el endpoint para bombardear un buzon.
      if (user.resetCodeSentAt &&
          Date.now() - user.resetCodeSentAt.getTime() < RESET_RESEND_COOLDOWN_MS) {
        return res.json({ ok: true });
      }

      const code = String(crypto.randomInt(0, 1000000)).padStart(6, '0');
      await prisma.user.update({
        where: { id: user.id },
        data: {
          resetCodeHash: await bcrypt.hash(code, 10),
          resetCodeExpiresAt: new Date(Date.now() + RESET_CODE_TTL_MS),
          resetCodeAttempts: 0,
          resetCodeSentAt: new Date()
        }
      });
      await sendPasswordResetCode(email, code, RESET_CODE_TTL_MS / 60000);
      return res.json({ ok: true });
    } catch (e) {
      console.error('[auth][forgot-password]', e?.message || e);
      return res.status(503).json({ error: 'service_unavailable' });
    }
  });

  // Paso 2: canjear el codigo por una contrasena nueva.
  router.post('/reset-password', async (req, res) => {
    const parse = resetSchema.safeParse(req.body || {});
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const email = parse.data.email.trim().toLowerCase();
    const { code, password } = parse.data;

    try {
      const user = await prisma.user.findUnique({ where: { email } });
      if (!user || !user.resetCodeHash || !user.resetCodeExpiresAt) {
        return res.status(400).json({ error: 'invalid_code' });
      }
      if (user.resetCodeExpiresAt.getTime() < Date.now()) {
        return res.status(400).json({ error: 'code_expired' });
      }
      if (user.resetCodeAttempts >= RESET_MAX_ATTEMPTS) {
        return res.status(429).json({ error: 'too_many_attempts' });
      }
      if (!(await bcrypt.compare(code, user.resetCodeHash))) {
        await prisma.user.update({
          where: { id: user.id },
          data: { resetCodeAttempts: { increment: 1 } }
        });
        return res.status(400).json({ error: 'invalid_code' });
      }

      await prisma.user.update({
        where: { id: user.id },
        data: {
          passwordHash: await bcrypt.hash(password, 10),
          passwordChangedAt: new Date(),
          // Recibir el codigo prueba que controla el correo.
          emailVerified: true,
          resetCodeHash: null,
          resetCodeExpiresAt: null,
          resetCodeAttempts: 0,
          resetCodeSentAt: null
        }
      });
      return res.json({ ok: true });
    } catch (e) {
      console.error('[auth][reset-password]', e?.message || e);
      return res.status(503).json({ error: 'service_unavailable' });
    }
  });

  // Verify email by token
  router.get('/verify', async (req, res) => {
    const token = req.query.token;
    if (!token || typeof token !== 'string') return res.status(400).send('invalid_token');
    const user = await prisma.user.findFirst({ where: { verificationToken: token } });
    if (!user) return res.status(400).send('invalid_token');
    if (!user.verificationSentAt || Date.now() - user.verificationSentAt.getTime() > VERIFICATION_TOKEN_TTL_MS) {
      return res.status(400).send('expired_token');
    }
    await prisma.user.update({ where: { id: user.id }, data: { emailVerified: true, verificationToken: null, verificationSentAt: null } });
    res.send('Email verificado. Ya puedes volver a la app e iniciar sesión.');
  });

  return router;
};

module.exports = routerFactory;

const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { z } = require('zod');
const crypto = require('crypto');
const { OAuth2Client } = require('google-auth-library');
const { sendVerificationEmail, sendVerificationLink, isEmailConfigured } = require('./email');

const VERIFICATION_TOKEN_TTL_MS = 24 * 60 * 60 * 1000;

const routerFactory = (prisma) => {
  const router = express.Router();
  const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret';
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

    try {
      const ticket = await googleClient.verifyIdToken({
        idToken: parse.data.idToken,
        audience: GOOGLE_CLIENT_ID,
      });
      const payload = ticket.getPayload();
      const email = payload && payload.email;
      const googleSub = payload && payload.sub;
      const name = payload && payload.name;
      if (!email || !googleSub || payload.email_verified === false) {
        return res.status(401).json({ error: 'invalid_token' });
      }

      const normalizedEmail = email.trim().toLowerCase();

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
      console.error('[auth][google] token verification failed:', e?.message || e);
      return res.status(401).json({ error: 'invalid_token' });
    }
  });

  router.post('/refresh', async (req, res) => {
    const { refreshToken } = req.body || {};
    if (!refreshToken) return res.status(400).json({ error: 'missing_token' });
    try {
      const payload = jwt.verify(refreshToken, JWT_SECRET);
      if (payload.typ !== 'refresh') throw new Error('bad_typ');
      const tokens = signTokens(payload.uid);
      res.json(tokens);
    } catch {
      res.status(401).json({ error: 'invalid_token' });
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

  // Send 6-digit verification code via email (does not persist the code yet)
  router.post('/send-code', async (req, res) => {
    const rawEmail = req.body?.email;
    const email = typeof rawEmail === 'string' ? rawEmail.trim().toLowerCase() : rawEmail;
    if (!email || typeof email !== 'string') return res.status(400).json({ error: 'invalid_body' });
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(404).json({ error: 'not_found' });
    const code = Math.floor(100000 + Math.random() * 900000);
    try {
      await sendVerificationEmail(email, code);
      res.json({ ok: true });
    } catch (err) {
      console.error('send-code error', err);
      res.status(500).json({ error: 'send_failed' });
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

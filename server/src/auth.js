const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { z } = require('zod');
const crypto = require('crypto');
const { sendVerificationEmail, sendVerificationLink } = require('./email');

const routerFactory = (prisma) => {
  const router = express.Router();
  const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret';

  const emailCreds = z.object({ email: z.string().email(), password: z.string().min(6) });
  const nameCreds  = z.object({ name: z.string().min(1), password: z.string().min(6) });
  const registerSchema = emailCreds.extend({
    name: z.string().min(1)
  });

  function signTokens(userId) {
    const accessToken = jwt.sign({ uid: userId }, JWT_SECRET, { expiresIn: '15m' });
    const refreshToken = jwt.sign({ uid: userId, typ: 'refresh' }, JWT_SECRET, { expiresIn: '30d' });
    return { accessToken, refreshToken };
  }

  async function sendVerification(prisma, user) {
    const token = crypto.randomUUID();
    await prisma.user.update({
      where: { id: user.id },
      data: { verificationToken: token, verificationSentAt: new Date() }
    });
    const baseUrl = process.env.APP_BASE_URL || 'http://localhost:3000';
    const link = `${baseUrl}/auth/verify?token=${encodeURIComponent(token)}`;
    try {
      await sendVerificationLink(user, link);
      return;
    } catch (e) {
      console.error('sendVerification error', e);
      console.log('[auth] verification link:', link);
      return;
    }
  }

  router.post('/register', async (req, res) => {
    const parse = registerSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const { email, password, name } = parse.data;
    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) return res.status(409).json({ error: 'email_in_use' });
    const hash = await bcrypt.hash(password, 10);
    const user = await prisma.user.create({ data: { email, passwordHash: hash, name, emailVerified: false } });
    try { await sendVerification(prisma, user); } catch (e) { console.error('sendVerification error', e); }
    res.json({ ok: true });
  });

  router.post('/login', async (req, res) => {
    const body = req.body || {};
    const useEmail = typeof body.email === 'string' && body.email.length > 0;
    const schema = useEmail ? emailCreds : nameCreds;
    const parse = schema.safeParse(body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const { password } = parse.data;
    const user = useEmail
      ? await prisma.user.findUnique({ where: { email: parse.data.email } })
      : await prisma.user.findFirst({ where: { name: parse.data.name } });
    if (!user) return res.status(401).json({ error: 'invalid_credentials' });
    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ error: 'invalid_credentials' });
    if (!user.emailVerified) return res.status(403).json({ error: 'email_not_verified' });
    const tokens = signTokens(user.id);
    res.json({ user: { id: user.id, email: user.email, name: user.name || null }, ...tokens });
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
    const { email } = req.body || {};
    if (!email || typeof email !== 'string') return res.status(400).json({ error: 'invalid_body' });
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(200).json({ ok: true });
    if (user.emailVerified) return res.status(200).json({ ok: true });
    try { await sendVerification(prisma, user); } catch (e) { console.error('sendVerification error', e); }
    res.json({ ok: true });
  });

  // Send 6-digit verification code via email (does not persist the code yet)
  router.post('/send-code', async (req, res) => {
    const { email } = req.body || {};
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
    await prisma.user.update({ where: { id: user.id }, data: { emailVerified: true, verificationToken: null, verificationSentAt: null } });
    res.send('Email verificado. Ya puedes volver a la app e iniciar sesión.');
  });

  return router;
};

module.exports = routerFactory;

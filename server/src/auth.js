const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { z } = require('zod');

const routerFactory = (prisma) => {
  const router = express.Router();
  const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret';

  const credsSchema = z.object({
    email: z.string().email(),
    password: z.string().min(6)
  });
  const registerSchema = credsSchema.extend({
    name: z.string().min(1)
  });

  function signTokens(userId) {
    const accessToken = jwt.sign({ uid: userId }, JWT_SECRET, { expiresIn: '15m' });
    const refreshToken = jwt.sign({ uid: userId, typ: 'refresh' }, JWT_SECRET, { expiresIn: '30d' });
    return { accessToken, refreshToken };
  }

  router.post('/register', async (req, res) => {
    const parse = registerSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const { email, password, name } = parse.data;
    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) return res.status(409).json({ error: 'email_in_use' });
    const hash = await bcrypt.hash(password, 10);
    const user = await prisma.user.create({ data: { email, passwordHash: hash, name } });
    const tokens = signTokens(user.id);
    res.json({ user: { id: user.id, email: user.email, name: user.name || null }, ...tokens });
  });

  router.post('/login', async (req, res) => {
    const parse = credsSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const { email, password } = parse.data;
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(401).json({ error: 'invalid_credentials' });
    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ error: 'invalid_credentials' });
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

  return router;
};

module.exports = routerFactory;

const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma) => {
  const router = express.Router();
  router.use(authMiddleware);

  const toSafeNumber = (value) => {
    if (typeof value === 'bigint') return Number(value);
    const n = Number(value);
    return Number.isFinite(n) ? n : Date.now();
  };

  const serializeMatch = (m) => ({
    ...m,
    time: toSafeNumber(m.time)
  });

  const matchSchema = z.object({
    time: z.number().int().nonnegative(),
    titleA: z.string().min(1),
    titleB: z.string().min(1),
    teamA: z.array(z.any()),
    teamB: z.array(z.any()),
    result: z.string().optional().default("")
  });

  router.get('/', async (req, res) => {
    const list = await prisma.match.findMany({ where: { userId: req.userId }, orderBy: { time: 'desc' } });
    res.json(list.map(serializeMatch));
  });

  router.post('/', async (req, res) => {
    const parse = matchSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const data = parse.data;
    const created = await prisma.match.create({ data: { ...data, time: BigInt(data.time), userId: req.userId } });
    res.status(201).json(serializeMatch(created));
  });

  router.put('/:id', async (req, res) => {
    const id = req.params.id;
    const parse = matchSchema.partial().safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const payload = { ...parse.data };
    if (payload.time !== undefined) payload.time = BigInt(payload.time);
    try {
      const updated = await prisma.match.update({ where: { id, userId: req.userId }, data: payload });
      res.json(serializeMatch(updated));
    } catch {
      res.status(404).json({ error: 'not_found' });
    }
  });

  router.delete('/:id', async (req, res) => {
    const id = req.params.id;
    try {
      await prisma.match.delete({ where: { id, userId: req.userId } });
      res.status(204).end();
    } catch {
      res.status(404).json({ error: 'not_found' });
    }
  });

  return router;
};

module.exports = routerFactory;

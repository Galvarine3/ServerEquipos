const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma) => {
  const router = express.Router();
  router.use(authMiddleware);

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
    res.json(list);
  });

  router.post('/', async (req, res) => {
    const parse = matchSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const data = parse.data;
    const created = await prisma.match.create({ data: { ...data, userId: req.userId } });
    res.status(201).json(created);
  });

  router.put('/:id', async (req, res) => {
    const id = req.params.id;
    const parse = matchSchema.partial().safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    try {
      const updated = await prisma.match.update({ where: { id, userId: req.userId }, data: parse.data });
      res.json(updated);
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

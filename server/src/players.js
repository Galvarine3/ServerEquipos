const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma) => {
  const router = express.Router();
  router.use(authMiddleware);

  const playerSchema = z.object({
    name: z.string().min(1),
    attack: z.number().min(1).max(10),
    defense: z.number().min(1).max(10),
    physical: z.number().min(1).max(10),
    isGoalkeeper: z.boolean().optional().default(false)
  });

  router.get('/', async (req, res) => {
    const list = await prisma.player.findMany({ where: { userId: req.userId }, orderBy: { name: 'asc' } });
    res.json(list);
  });

  router.post('/', async (req, res) => {
    const parse = playerSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const data = parse.data;
    try {
      const created = await prisma.player.create({ data: { ...data, userId: req.userId } });
      res.status(201).json(created);
    } catch (e) {
      res.status(400).json({ error: 'create_failed' });
    }
  });

  router.put('/:id', async (req, res) => {
    const parse = playerSchema.partial().safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const id = req.params.id;
    try {
      const updated = await prisma.player.update({ where: { id, userId: req.userId }, data: parse.data });
      res.json(updated);
    } catch {
      res.status(404).json({ error: 'not_found' });
    }
  });

  router.delete('/:id', async (req, res) => {
    const id = req.params.id;
    try {
      await prisma.player.delete({ where: { id, userId: req.userId } });
      res.status(204).end();
    } catch {
      res.status(404).json({ error: 'not_found' });
    }
  });

  router.post('/bulk', async (req, res) => {
    const body = req.body;
    const arr = Array.isArray(body) ? body : (Array.isArray(body?.players) ? body.players : []);
    const parsed = arr.map(p => playerSchema.safeParse(p)).filter(r => r.success).map(r => r.data);

    if (parsed.length === 0) {
      await prisma.player.deleteMany({ where: { userId: req.userId } });
      return res.json([]);
    }

    const names = parsed.map(p => p.name);

    await prisma.$transaction([
      prisma.player.deleteMany({
        where: {
          userId: req.userId,
          name: { notIn: names }
        }
      }),
      ...parsed.map(p =>
        prisma.player.upsert({
          where: { userId_name: { userId: req.userId, name: p.name } },
          update: { attack: p.attack, defense: p.defense, physical: p.physical, isGoalkeeper: !!p.isGoalkeeper },
          create: { ...p, userId: req.userId }
        })
      )
    ]);

    const list = await prisma.player.findMany({ where: { userId: req.userId }, orderBy: { name: 'asc' } });
    res.json(list);
  });

  return router;
};

module.exports = routerFactory;

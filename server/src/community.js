const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma, hub) => {
  const router = express.Router();
  router.use(authMiddleware);

  // Helper: convert BigInt fields to JSON-serializable values for the client
  const toClientPost = (post) => {
    if (!post) return post;
    return {
      ...post,
      // Prisma maps BigInt to JS BigInt, which JSON.stringify cannot handle
      time: typeof post.time === 'bigint' ? Number(post.time) : post.time,
    };
  };

  const postSchema = z.object({
    userName: z.string().min(0).default(''),
    sport: z.string().min(1),
    available: z.number().int().positive(),
    total: z.number().int().positive(),
    message: z.string().min(0).default(''),
    locality: z.string().min(1),
    time: z.bigint().or(z.number().int()).transform(v => BigInt(v))
  }).refine(d => d.available <= d.total, { message: 'available_lte_total' });

  router.get('/posts', async (_req, res) => {
    const list = await prisma.communityPost.findMany({
      orderBy: { time: 'desc' }
    });
    res.json(list.map(toClientPost));
  });

  router.post('/posts', async (req, res) => {
    const parse = postSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const data = parse.data;
    try {
      const created = await prisma.communityPost.create({
        data: {
          userId: req.userId,
          userName: data.userName || '',
          sport: data.sport,
          available: data.available,
          total: data.total,
          message: data.message || '',
          locality: data.locality,
          time: data.time
        }
      });
      const forClient = toClientPost(created);
      try { hub && hub.postCreated && hub.postCreated(forClient); } catch (_) {}
      res.status(201).json(forClient);
    } catch (e) {
      res.status(400).json({ error: 'create_failed' });
    }
  });

  router.put('/posts/:id', async (req, res) => {
    const id = req.params.id;
    const parse = postSchema.partial().safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const data = parse.data;
    try {
      const existing = await prisma.communityPost.findUnique({ where: { id } });
      if (!existing || existing.userId !== req.userId) return res.status(404).json({ error: 'not_found' });
      const updated = await prisma.communityPost.update({ where: { id }, data });
      const forClient = toClientPost(updated);
      try { hub && hub.postUpdated && hub.postUpdated(forClient); } catch (_) {}
      res.json(forClient);
    } catch (e) {
      res.status(400).json({ error: 'update_failed' });
    }
  });

  router.delete('/posts/:id', async (req, res) => {
    const id = req.params.id;
    try {
      const existing = await prisma.communityPost.findUnique({ where: { id } });
      if (!existing || existing.userId !== req.userId) return res.status(404).json({ error: 'not_found' });
      await prisma.communityPost.delete({ where: { id } });
      try { hub && hub.postDeleted && hub.postDeleted(id); } catch (_) {}
      res.status(204).end();
    } catch (e) {
      res.status(404).json({ error: 'not_found' });
    }
  });

  return router;
};

module.exports = routerFactory;

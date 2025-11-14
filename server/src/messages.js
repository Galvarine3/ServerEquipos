const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma, hub) => {
  const router = express.Router();
  router.use(authMiddleware);

  // GET /messages?withUser=<id>
  router.get('/', async (req, res) => {
    const otherId = (req.query.withUser || '').toString();
    if (!otherId) return res.status(400).json({ error: 'withUser_required' });
    const list = await prisma.message.findMany({
      where: {
        OR: [
          { fromUserId: req.userId, toUserId: otherId },
          { fromUserId: otherId, toUserId: req.userId }
        ]
      },
      orderBy: { time: 'asc' }
    });
    res.json(list);
  });

  // POST /messages
  const postSchema = z.object({
    toUserId: z.string().min(1),
    text: z.string().min(1),
    toName: z.string().optional().default(''),
    fromName: z.string().optional().default(''),
    time: z.bigint().or(z.number().int()).optional()
  });

  router.post('/', async (req, res) => {
    const parse = postSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const data = parse.data;
    try {
      const saved = await prisma.message.create({
        data: {
          fromUserId: req.userId,
          toUserId: data.toUserId,
          fromName: data.fromName || '',
          toName: data.toName || '',
          text: data.text,
          time: BigInt(data.time || Date.now())
        }
      });
      try { hub && hub.sendToUser && hub.sendToUser(data.toUserId, { type: 'message_new', data: saved }); } catch (_) {}
      res.status(201).json(saved);
    } catch (e) {
      res.status(400).json({ error: 'create_failed' });
    }
  });

  return router;
};

module.exports = routerFactory;

const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma, hub) => {
  const router = express.Router();
  router.use(authMiddleware);

  // Helper: convert BigInt fields to JSON-serializable values
  const toClientMessage = (m) => {
    if (!m) return m;
    return {
      ...m,
      time: typeof m.time === 'bigint' ? Number(m.time) : m.time,
    };
  };

  // GET /messages?withUser=<id>&postId=<postId>
  router.get('/', async (req, res) => {
    const otherId = (req.query.withUser || '').toString();
    const postId = (req.query.postId || '').toString();
    if (!otherId) return res.status(400).json({ error: 'withUser_required' });
    if (!postId) return res.status(400).json({ error: 'postId_required' });
    const list = await prisma.message.findMany({
      where: {
        postId,
        OR: [
          { fromUserId: req.userId, toUserId: otherId },
          { fromUserId: otherId, toUserId: req.userId }
        ]
      },
      orderBy: { time: 'asc' }
    });
    res.json(list.map(toClientMessage));
  });

  // POST /messages
  const postSchema = z.object({
    toUserId: z.string().min(1),
    text: z.string().min(1),
    toName: z.string().optional().default(''),
    fromName: z.string().optional().default(''),
    time: z.bigint().or(z.number().int()).optional(),
    postId: z.string().min(1)
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
          time: BigInt(data.time || Date.now()),
          postId: data.postId
        }
      });
      const clientMsg = toClientMessage(saved);
      try {
        if (hub && hub.sendToUser) {
          hub.sendToUser(data.toUserId, { type: 'message_new', data: clientMsg });
          hub.sendToUser(req.userId, { type: 'message_new', data: clientMsg });
        }
      } catch (_) {}
      res.status(201).json(clientMsg);
    } catch (e) {
      res.status(400).json({ error: 'create_failed' });
    }
  });

  return router;
};

module.exports = routerFactory;

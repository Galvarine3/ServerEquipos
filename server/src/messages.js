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

  const toClientPostMessage = (m) => {
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
    return res.end();
  });

  // Shared post chat: GET history
  router.get('/posts/:id', async (req, res) => {
    const postId = req.params.id;
    if (!postId) return res.status(400).json({ error: 'postId_required' });
    const list = await prisma.postMessage.findMany({
      where: { postId },
      orderBy: { time: 'asc' }
    });
    res.json(list.map(toClientPostMessage));
  });

  // Shared post chat: POST new message
  const postGroupSchema = z.object({
    text: z.string().min(1),
    fromName: z.string().optional().default(''),
    time: z.bigint().or(z.number().int()).optional()
  });

  router.post('/posts/:id', async (req, res) => {
    const postId = req.params.id;
    const parse = postGroupSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    try {
      const data = parse.data;
      const saved = await prisma.postMessage.create({
        data: {
          postId,
          fromUserId: req.userId,
          fromName: data.fromName || '',
          text: data.text,
          time: BigInt(data.time || Date.now())
        }
      });
      const msg = toClientPostMessage(saved);
      try { hub && hub.postMessageNew && hub.postMessageNew(msg); } catch (_) {}
      res.status(201).json(msg);
    } catch (e) {
      res.status(400).json({ error: 'create_failed' });
    }
  });
  

  // GET /messages/threads?postId=<id>
  router.get('/threads', async (req, res) => {
    const postId = (req.query.postId || '').toString();
    if (!postId) return res.status(400).json({ error: 'postId_required' });
    const uid = req.userId;
    const list = await prisma.message.findMany({
      where: {
        postId,
        OR: [
          { fromUserId: uid },
          { toUserId: uid }
        ]
      },
      orderBy: { time: 'desc' }
    });
    // Reduce to distinct partner threads with latest message
    const threads = new Map(); // partnerId -> { userId, userName, lastText, time }
    for (const m of list) {
      const isOutgoing = m.fromUserId === uid;
      const partnerId = isOutgoing ? m.toUserId : m.fromUserId;
      const partnerName = isOutgoing ? (m.toName || '') : (m.fromName || '');
      const t = typeof m.time === 'bigint' ? Number(m.time) : m.time;
      if (!threads.has(partnerId) || threads.get(partnerId).time < t) {
        threads.set(partnerId, { userId: partnerId, userName: partnerName, lastText: m.text, time: t });
      }
    }
    const arr = Array.from(threads.values()).sort((a, b) => b.time - a.time);
    res.json(arr);
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
          // Don't enforce CommunityPost relation here; DM uses synthetic postId (e.g. dm:<a>|<b>)
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
      res.status(400).json({ error: 'create_failed', detail: (e && e.message) ? String(e.message).slice(0, 200) : '' });
    }
  });

  return router;
};

module.exports = routerFactory;

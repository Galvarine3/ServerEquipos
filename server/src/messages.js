const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma, hub) => {
  const router = express.Router();
  router.use(authMiddleware);

  const parseLimit = (v, { def = 200, max = 500 } = {}) => {
    const n = Number.parseInt((v ?? '').toString(), 10);
    if (!Number.isFinite(n) || n <= 0) return def;
    return Math.min(n, max);
  };

  const parseBefore = (v) => {
    const n = Number.parseInt((v ?? '').toString(), 10);
    if (!Number.isFinite(n) || n <= 0) return null;
    return BigInt(n);
  };

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

    // Pagination: return the most recent messages by default (prevents OOM on large threads)
    const limit = parseLimit(req.query.limit, { def: 200, max: 500 });
    const before = parseBefore(req.query.before);

    const list = await prisma.message.findMany({
      where: {
        postId,
        OR: [
          { fromUserId: req.userId, toUserId: otherId },
          { fromUserId: otherId, toUserId: req.userId }
        ],
        ...(before ? { time: { lt: before } } : {})
      },
      orderBy: { time: 'desc' },
      take: limit
    });

    // Client expects ascending order
    res.json(list.reverse().map(toClientMessage));
  });

  // Shared post chat: GET history
  router.get('/posts/:id', async (req, res) => {
    const postId = req.params.id;
    if (!postId) return res.status(400).json({ error: 'postId_required' });

    const limit = parseLimit(req.query.limit, { def: 200, max: 500 });
    const before = parseBefore(req.query.before);

    const list = await prisma.postMessage.findMany({
      where: { postId, ...(before ? { time: { lt: before } } : {}) },
      orderBy: { time: 'desc' },
      take: limit
    });

    res.json(list.reverse().map(toClientPostMessage));
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

    // Cap scanned messages for performance; list is ordered desc so first per partner is latest.
    const scanLimit = parseLimit(req.query.scanLimit, { def: 5000, max: 20000 });

    const list = await prisma.message.findMany({
      where: {
        postId,
        OR: [
          { fromUserId: uid },
          { toUserId: uid }
        ]
      },
      orderBy: { time: 'desc' },
      take: scanLimit
    });
    // Reduce to distinct partner threads with latest message
    const threads = new Map(); // partnerId -> { userId, userName, lastText, time }
    for (const m of list) {
      const isOutgoing = m.fromUserId === uid;
      const partnerId = isOutgoing ? m.toUserId : m.fromUserId;
      if (threads.has(partnerId)) continue; // already have newest (list is desc)
      const partnerName = isOutgoing ? (m.toName || '') : (m.fromName || '');
      const t = typeof m.time === 'bigint' ? Number(m.time) : m.time;
      threads.set(partnerId, { userId: partnerId, userName: partnerName, lastText: m.text, time: t });
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

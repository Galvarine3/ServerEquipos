const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const routerFactory = (prisma) => {
  const router = express.Router();
  router.use(authMiddleware);

  const requestSchema = z.object({
    toUserId: z.string().min(1)
  });

  const nameFromUser = (u, fallback = '') => {
    if (!u) return fallback;
    return u.name || u.email || fallback;
  };

  // GET /friends - list accepted friends (agenda)
  router.get('/', async (req, res) => {
    const list = await prisma.friend.findMany({
      where: { userId: req.userId },
      include: { friend: { select: { id: true, name: true, email: true } } },
      orderBy: { createdAt: 'desc' }
    });
    res.json(list.map((f) => ({
      userId: f.friendUserId,
      name: nameFromUser(f.friend),
      since: f.createdAt
    })));
  });

  // GET /friends/requests - incoming requests
  router.get('/requests', async (req, res) => {
    const list = await prisma.friendRequest.findMany({
      where: { toUserId: req.userId },
      include: { fromUser: { select: { id: true, name: true, email: true } } },
      orderBy: { createdAt: 'desc' }
    });
    res.json(list.map((r) => ({
      id: r.id,
      fromUserId: r.fromUserId,
      fromName: nameFromUser(r.fromUser),
      createdAt: r.createdAt
    })));
  });

  // GET /friends/requests/outgoing - outgoing pending requests
  router.get('/requests/outgoing', async (req, res) => {
    const list = await prisma.friendRequest.findMany({
      where: { fromUserId: req.userId },
      include: { toUser: { select: { id: true, name: true, email: true } } },
      orderBy: { createdAt: 'desc' }
    });
    res.json(list.map((r) => ({
      id: r.id,
      toUserId: r.toUserId,
      toName: nameFromUser(r.toUser),
      createdAt: r.createdAt
    })));
  });

  // POST /friends/requests - send request
  router.post('/requests', async (req, res) => {
    const parse = requestSchema.safeParse(req.body);
    if (!parse.success) return res.status(400).json({ error: 'invalid_body' });
    const { toUserId } = parse.data;
    if (toUserId === req.userId) return res.status(400).json({ error: 'self_request' });

    const target = await prisma.user.findUnique({ where: { id: toUserId } });
    if (!target) return res.status(404).json({ error: 'user_not_found' });

    const alreadyFriend = await prisma.friend.findFirst({
      where: { userId: req.userId, friendUserId: toUserId }
    });
    if (alreadyFriend) return res.status(409).json({ error: 'already_friends' });

    const incoming = await prisma.friendRequest.findFirst({
      where: { fromUserId: toUserId, toUserId: req.userId }
    });
    if (incoming) {
      try {
        await prisma.$transaction([
          prisma.friend.create({ data: { userId: req.userId, friendUserId: toUserId } }),
          prisma.friend.create({ data: { userId: toUserId, friendUserId: req.userId } }),
          prisma.friendRequest.delete({ where: { id: incoming.id } })
        ]);
        return res.json({ status: 'accepted' });
      } catch (e) {
        return res.status(400).json({ error: 'accept_failed' });
      }
    }

    try {
      const created = await prisma.friendRequest.create({
        data: { fromUserId: req.userId, toUserId }
      });
      return res.status(201).json({ id: created.id, status: 'sent' });
    } catch (e) {
      return res.status(409).json({ error: 'already_requested' });
    }
  });

  // POST /friends/requests/:id/accept
  router.post('/requests/:id/accept', async (req, res) => {
    const id = req.params.id;
    const existing = await prisma.friendRequest.findUnique({ where: { id } });
    if (!existing || existing.toUserId !== req.userId) {
      return res.status(404).json({ error: 'not_found' });
    }
    try {
      await prisma.$transaction([
        prisma.friend.create({ data: { userId: existing.fromUserId, friendUserId: existing.toUserId } }),
        prisma.friend.create({ data: { userId: existing.toUserId, friendUserId: existing.fromUserId } }),
        prisma.friendRequest.delete({ where: { id } })
      ]);
      return res.json({ status: 'accepted' });
    } catch (e) {
      return res.status(400).json({ error: 'accept_failed' });
    }
  });

  // POST /friends/requests/:id/decline
  router.post('/requests/:id/decline', async (req, res) => {
    const id = req.params.id;
    const existing = await prisma.friendRequest.findUnique({ where: { id } });
    if (!existing || existing.toUserId !== req.userId) {
      return res.status(404).json({ error: 'not_found' });
    }
    try {
      await prisma.friendRequest.delete({ where: { id } });
      return res.json({ status: 'declined' });
    } catch (e) {
      return res.status(400).json({ error: 'decline_failed' });
    }
  });

  // DELETE /friends/:userId - remove friend (agenda)
  router.delete('/:userId', async (req, res) => {
    const targetUserId = req.params.userId;
    if (!targetUserId || targetUserId === req.userId) {
      return res.status(400).json({ error: 'invalid_user' });
    }

    const target = await prisma.user.findUnique({ where: { id: targetUserId } });
    if (!target) return res.status(404).json({ error: 'user_not_found' });

    const existing = await prisma.friend.findFirst({
      where: { userId: req.userId, friendUserId: targetUserId }
    });
    if (!existing) return res.status(404).json({ error: 'not_friends' });

    try {
      await prisma.$transaction([
        prisma.friend.deleteMany({ where: { userId: req.userId, friendUserId: targetUserId } }),
        prisma.friend.deleteMany({ where: { userId: targetUserId, friendUserId: req.userId } }),
        prisma.friendRequest.deleteMany({
          where: {
            OR: [
              { fromUserId: req.userId, toUserId: targetUserId },
              { fromUserId: targetUserId, toUserId: req.userId }
            ]
          }
        })
      ]);
      return res.json({ status: 'deleted' });
    } catch (e) {
      return res.status(400).json({ error: 'delete_failed' });
    }
  });

  return router;
};

module.exports = routerFactory;

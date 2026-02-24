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

  const EARTH_RADIUS_KM = 6371;
  const toRadians = (deg) => (deg * Math.PI) / 180;
  const haversineKm = (lat1, lon1, lat2, lon2) => {
    const dLat = toRadians(lat2 - lat1);
    const dLon = toRadians(lon2 - lon1);
    const a = Math.sin(dLat / 2) ** 2 +
      Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
      Math.sin(dLon / 2) ** 2;
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
  };

  const postSchema = z.object({
    userName: z.string().min(0).default(''),
    sport: z.string().min(1),
    available: z.number().int().positive(),
    total: z.number().int().positive(),
    message: z.string().min(0).default(''),
    locality: z.string().min(1),
    latitude: z.number().min(-90).max(90).optional(),
    longitude: z.number().min(-180).max(180).optional(),
    time: z.bigint().or(z.number().int()).transform(v => BigInt(v))
  })
    .refine(d => d.available == null || d.total == null || d.available <= d.total, { message: 'available_lte_total' })
    .refine(d => (d.latitude == null && d.longitude == null) || (d.latitude != null && d.longitude != null), { message: 'lat_lng_pair' });

  const nearbyQuerySchema = z.object({
    lat: z.coerce.number().min(-90).max(90),
    lng: z.coerce.number().min(-180).max(180),
    radiusKm: z.coerce.number().positive().max(200).default(10),
    limit: z.coerce.number().int().positive().max(200).optional()
  });

  router.get('/posts', async (_req, res) => {
    const list = await prisma.communityPost.findMany({
      orderBy: { time: 'desc' }
    });
    res.json(list.map(toClientPost));
  });

  router.get('/posts/nearby', async (req, res) => {
    const parse = nearbyQuerySchema.safeParse(req.query);
    if (!parse.success) return res.status(400).json({ error: 'invalid_query' });
    const { lat, lng, radiusKm, limit } = parse.data;
    const latDelta = radiusKm / 111.32;
    const cosLat = Math.cos(toRadians(lat));
    const lngDelta = radiusKm / (111.32 * (cosLat === 0 ? 1 : cosLat));
    const candidateLimit = Math.min((limit || 50) * 5, 500);

    const candidates = await prisma.communityPost.findMany({
      where: {
        latitude: { not: null, gte: lat - latDelta, lte: lat + latDelta },
        longitude: { not: null, gte: lng - lngDelta, lte: lng + lngDelta }
      },
      orderBy: { time: 'desc' },
      take: candidateLimit
    });

    const results = candidates
      .map((post) => {
        const distanceKm = haversineKm(lat, lng, post.latitude, post.longitude);
        return { ...toClientPost(post), distanceKm };
      })
      .filter((post) => post.distanceKm <= radiusKm)
      .sort((a, b) => (a.distanceKm - b.distanceKm) || (b.time - a.time));

    res.json(limit ? results.slice(0, limit) : results);
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
          latitude: data.latitude ?? null,
          longitude: data.longitude ?? null,
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
      // delete related messages first (because of FK)
      await prisma.message.deleteMany({ where: { postId: id } });
      await prisma.communityPost.delete({ where: { id } });
      try { hub && hub.postDeleted && hub.postDeleted(id); } catch (_) {}
      res.status(204).end();
    } catch (e) {
      res.status(404).json({ error: 'not_found' });
    }
  });

  // ADMIN: delete all posts (and related messages)
  router.delete('/posts', async (req, res) => {
    const ADMIN_USER_ID = process.env.ADMIN_USER_ID || '';
    if (!ADMIN_USER_ID || req.userId !== ADMIN_USER_ID) return res.status(403).json({ error: 'forbidden' });
    // collect ids to broadcast after deletion
    const posts = await prisma.communityPost.findMany({ select: { id: true } });
    const ids = posts.map(p => p.id);
    try {
      // delete messages tied to those posts first
      if (ids.length > 0) await prisma.message.deleteMany({ where: { postId: { in: ids } } });
      const delRes = await prisma.communityPost.deleteMany({});
      try {
        if (hub && hub.postDeleted) ids.forEach(id => hub.postDeleted(id));
      } catch (_) {}
      res.json({ deleted: delRes.count });
    } catch (e) {
      res.status(400).json({ error: 'delete_failed' });
    }
  });

  return router;
};

module.exports = routerFactory;

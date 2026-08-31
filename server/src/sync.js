const express = require('express');
const { z } = require('zod');
const { authMiddleware } = require('./middleware');

const id = z.union([z.string(), z.number()]).transform(String);
const player = z.object({ name: z.string().min(1), attack: z.number(), defense: z.number(), physical: z.number(), isGoalkeeper: z.boolean().optional() });
const team = z.object({ id, name: z.string().min(1), players: z.array(z.any()) });
const match = z.object({ id, time: z.number().int().nonnegative(), titleA: z.string(), titleB: z.string(), teamA: z.array(z.any()), teamB: z.array(z.any()), result: z.string().optional().default('') });
const competition = z.object({ id, time: z.number().int().nonnegative(), name: z.string() }).passthrough();

module.exports = function syncRoutes(prisma) {
  const router = express.Router();
  router.use(authMiddleware);

  router.get('/', async (req, res) => {
    const [players, matches, teams, tournaments, leagues] = await Promise.all([
      prisma.player.findMany({ where: { userId: req.userId }, orderBy: { name: 'asc' } }),
      prisma.match.findMany({ where: { userId: req.userId }, orderBy: { time: 'desc' } }),
      prisma.team.findMany({ where: { userId: req.userId }, orderBy: { updatedAt: 'desc' } }),
      prisma.tournament.findMany({ where: { userId: req.userId }, orderBy: { time: 'desc' } }),
      prisma.league.findMany({ where: { userId: req.userId }, orderBy: { time: 'desc' } })
    ]);
    res.json({ players, matches: matches.map(m => ({ id: m.externalId, time: Number(m.time), titleA: m.titleA, titleB: m.titleB, teamA: m.teamA, teamB: m.teamB, result: m.result })),
      teams: teams.map(t => ({ id: t.externalId, name: t.name, players: t.players })),
      tournaments: tournaments.map(t => ({ id: t.externalId, time: Number(t.time), name: t.name, ...t.data })),
      leagues: leagues.map(l => ({ id: l.externalId, time: Number(l.time), name: l.name, ...l.data })) });
  });

  router.put('/', async (req, res) => {
    const body = req.body || {};
    const players = z.array(player).safeParse(body.players || []).data || [];
    const matches = z.array(match).safeParse(body.matches || []).data || [];
    const teams = z.array(team).safeParse(body.teams || []).data || [];
    const tournaments = z.array(competition).safeParse(body.tournaments || []).data || [];
    const leagues = z.array(competition).safeParse(body.leagues || []).data || [];
    await prisma.$transaction(async tx => {
      await tx.player.deleteMany({ where: { userId: req.userId } });
      await tx.match.deleteMany({ where: { userId: req.userId } });
      await tx.team.deleteMany({ where: { userId: req.userId } });
      await tx.tournament.deleteMany({ where: { userId: req.userId } });
      await tx.league.deleteMany({ where: { userId: req.userId } });
      if (players.length) await tx.player.createMany({ data: players.map(p => ({ ...p, userId: req.userId })) });
      if (matches.length) await tx.match.createMany({ data: matches.map(m => ({ externalId: m.id, time: BigInt(m.time), titleA: m.titleA, titleB: m.titleB, teamA: m.teamA, teamB: m.teamB, result: m.result, userId: req.userId })) });
      if (teams.length) await tx.team.createMany({ data: teams.map(t => ({ externalId: t.id, name: t.name, players: t.players, userId: req.userId })) });
      if (tournaments.length) await tx.tournament.createMany({ data: tournaments.map(t => ({ externalId: t.id, name: t.name, time: BigInt(t.time), data: t, userId: req.userId })) });
      if (leagues.length) await tx.league.createMany({ data: leagues.map(l => ({ externalId: l.id, name: l.name, time: BigInt(l.time), data: l, userId: req.userId })) });
    });
    res.status(204).end();
  });
  return router;
};

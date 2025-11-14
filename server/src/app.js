require('dotenv').config();
const express = require('express');
const http = require('http');
const cors = require('cors');
const { PrismaClient } = require('@prisma/client');
const authRoutes = require('./auth');
const playersRoutes = require('./players');
const matchesRoutes = require('./matches');
const communityRoutes = require('./community');
const messagesRoutes = require('./messages');
const { initWS } = require('./ws');

const app = express();
const server = http.createServer(app);
const prisma = new PrismaClient();

app.use(cors());
app.use(express.json());

app.get('/health', (_req, res) => res.json({ ok: true }));

// Root endpoint: helpful message
app.get('/', (_req, res) => {
  res.json({
    name: 'Equipos API',
    status: 'ok',
    health: '/health',
    auth: ['/auth/register', '/auth/login', '/auth/refresh'],
    resources: ['/players', '/matches']
  });
});

// Routes
const hub = initWS(server, prisma);
app.use('/auth', authRoutes(prisma));
app.use('/players', playersRoutes(prisma));
app.use('/matches', matchesRoutes(prisma));
app.use('/community', communityRoutes(prisma, hub));
app.use('/messages', messagesRoutes(prisma, hub));

const port = process.env.PORT || 3000;
server.listen(port, () => {
  console.log(`API + WS running on port ${port}`);
});

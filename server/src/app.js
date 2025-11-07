require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { PrismaClient } = require('@prisma/client');
const authRoutes = require('./auth');
const playersRoutes = require('./players');
const matchesRoutes = require('./matches');

const app = express();
const prisma = new PrismaClient();

app.use(cors());
app.use(express.json());

app.get('/health', (_req, res) => res.json({ ok: true }));

// Routes
app.use('/auth', authRoutes(prisma));
app.use('/players', playersRoutes(prisma));
app.use('/matches', matchesRoutes(prisma));

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`API running on port ${port}`);
});

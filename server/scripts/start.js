#!/usr/bin/env node
const { spawnSync } = require('node:child_process');
const path = require('node:path');

require('dotenv').config();

function normalizeDatabaseUrl(url) {
  if (!url || typeof url !== 'string') return url;
  try {
    const parsed = new URL(url);
    if (!parsed.searchParams.has('sslmode')) parsed.searchParams.set('sslmode', 'require');
    return parsed.toString();
  } catch {
    const hasQuery = url.includes('?');
    const hasSslmode = /(^|[?&])sslmode=/.test(url);
    if (hasSslmode) return url;
    return `${url}${hasQuery ? '&' : '?'}sslmode=require`;
  }
}

const rawUrl = process.env.DATABASE_PUBLIC_URL;
const normalizedUrl = normalizeDatabaseUrl(rawUrl);

if (rawUrl && normalizedUrl !== rawUrl) {
  process.env.DATABASE_PUBLIC_URL = normalizedUrl;
  console.log('DATABASE_PUBLIC_URL normalized (added sslmode=require)');
}

let prismaCliPath;
try {
  prismaCliPath = require.resolve('prisma/build/index.js');
} catch {
  console.error('Prisma CLI not found. Ensure `prisma` is installed (dependencies or devDependencies).');
  process.exit(1);
}

const prisma = spawnSync(process.execPath, [prismaCliPath, 'migrate', 'deploy'], {
  stdio: 'inherit',
  env: process.env,
  cwd: path.join(__dirname, '..')
});

process.exitCode = prisma.status ?? 1;
if (process.exitCode !== 0) process.exit(process.exitCode);

require('../src/app');

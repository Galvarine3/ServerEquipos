require("dotenv").config();

const http = require("http");
const express = require("express");
const { PrismaClient } = require("@prisma/client");
const { Server } = require("colyseus");
const { CrashBallsRoom } = require("./game/CrashBallsRoom");

async function bootstrap() {
  const prisma = new PrismaClient();
  const app = express();
  const server = http.createServer(app);
  const gameServer = new Server({
    server,
    greet: false,
  });

  app.get("/health", (_req, res) => {
    res.json({
      ok: true,
      service: "crash-balls-colyseus",
    });
  });

  gameServer
    .define("crash-balls", CrashBallsRoom, { prisma })
    .filterBy(["roomKey"])
    .enableRealtimeListing();

  const port = Number(process.env.GAME_PORT || 3001);
  await gameServer.listen(port);
  console.log(`Crash Balls game server running on port ${port}`);
}

bootstrap().catch((error) => {
  console.error("Failed to start Crash Balls game server", error);
  process.exit(1);
});

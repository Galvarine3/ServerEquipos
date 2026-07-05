require("dotenv").config();

const http = require("http");
const express = require("express");
const { WebSocketServer } = require("ws");
const { PrismaClient } = require("@prisma/client");
const { Server } = require("colyseus");
const { matchMaker } = require("@colyseus/core");
const { CrashBallsRoom } = require("./game/CrashBallsRoom");
const { verifyAccessToken } = require("./game/auth");

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

  app.get("/bridge-health", (_req, res) => {
    res.json({
      ok: true,
      service: "crash-balls-bridge",
    });
  });

  gameServer
    .define("crash-balls", CrashBallsRoom, { prisma })
    .filterBy(["roomKey"])
    .enableRealtimeListing();

  const port = Number(process.env.GAME_PORT || 3001);
  const bridgePort = Number(process.env.GAME_BRIDGE_PORT || 3002);
  const bridgeServer = http.createServer();
  const bridgeWss = new WebSocketServer({ server: bridgeServer, path: "/crash-balls-bridge" });

  bridgeWss.on("connection", (ws) => {
    let joinedUserId = null;
    let joinedRoom = null;

    ws.on("message", async (raw) => {
      try {
        const message = JSON.parse(raw.toString());

        if (message.type === "join_room") {
          const roomKey = String(message.roomKey || "").trim();
          const payload = verifyAccessToken(String(message.token || ""));
          const user = await prisma.user.findUnique({
            where: { id: payload.uid },
            select: { id: true, name: true },
          });

          if (!roomKey || !user) {
            ws.send(JSON.stringify({ type: "error", data: { message: "invalid_join" } }));
            return;
          }

          let listing = await matchMaker.findOneRoomAvailable("crash-balls", { roomKey });
          if (!listing) {
            listing = await matchMaker.createRoom("crash-balls", { roomKey });
          }

          const room = matchMaker.getLocalRoomById(listing.roomId);
          if (!room || typeof room.bridgeJoin !== "function") {
            ws.send(JSON.stringify({ type: "error", data: { message: "room_unavailable" } }));
            return;
          }

          joinedUserId = user.id;
          joinedRoom = room;
          room.bridgeJoin(ws, {
            userId: user.id,
            name: user.name || "Jugador",
          });
          return;
        }

        if (!joinedUserId || !joinedRoom) {
          ws.send(JSON.stringify({ type: "error", data: { message: "join_required" } }));
          return;
        }

        if (message.type === "input") {
          joinedRoom.bridgeInput(joinedUserId, message.data || message);
        } else if (message.type === "ping") {
          ws.send(JSON.stringify({
            type: "pong",
            data: {
              now: Date.now(),
              echo: Number(message.now || message.data?.now) || 0,
            },
          }));
        }
      } catch (_error) {
        try {
          ws.send(JSON.stringify({ type: "error", data: { message: "invalid_payload" } }));
        } catch (_sendError) {}
      }
    });

    ws.on("close", () => {
      if (joinedUserId && joinedRoom && typeof joinedRoom.bridgeLeave === "function") {
        joinedRoom.bridgeLeave(joinedUserId);
      }
    });
  });

  await gameServer.listen(port);
  bridgeServer.listen(bridgePort, () => {
    console.log(`Crash Balls bridge server running on port ${bridgePort}`);
  });
  console.log(`Crash Balls game server running on port ${port}`);
}

bootstrap().catch((error) => {
  console.error("Failed to start Crash Balls game server", error);
  process.exit(1);
});

const { Room } = require("colyseus");
const { CrashBallsRoomState, CrashBallsPlayerState } = require("./CrashBallsState");
const { verifyAccessToken } = require("./auth");

const FIELD_WIDTH = 1000;
const FIELD_HEIGHT = 600;
const HALF_WIDTH = FIELD_WIDTH / 2;
const PLAYER_RADIUS = 52;
const BALL_RADIUS = 22;
const GOAL_TOP = 190;
const GOAL_BOTTOM = 410;
const LEFT_GOAL_X = 28;
const RIGHT_GOAL_X = FIELD_WIDTH - 28;
const MAX_SPEED = 540;
const ACCEL = 1700;
const PLAYER_DAMPING = 0.87;
const BALL_DAMPING = 0.992;
const KICK_POWER = 680;
const COUNTDOWN_TICKS = 120;
const MAX_SCORE = 3;

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function normalizeVector(x, y) {
  const length = Math.hypot(x, y);
  if (length <= 0.0001) {
    return { x: 0, y: 0 };
  }

  return { x: x / length, y: y / length };
}

function isGoalY(y) {
  return y >= GOAL_TOP && y <= GOAL_BOTTOM;
}

class CrashBallsRoom extends Room {
  async onAuth(_client, _options, context) {
    const token = context?.token;
    const payload = verifyAccessToken(token);
    const user = await this.prisma?.user?.findUnique({
      where: { id: payload.uid },
      select: { id: true, name: true },
    });

    return {
      userId: payload.uid,
      name: user?.name || "Jugador",
    };
  }

  onCreate(options = {}) {
    this.prisma = options.prisma || null;
    this.maxClients = 2;
    this.autoDispose = true;
    this.setState(new CrashBallsRoomState());
    this.state.roomKey = String(options.roomKey || "");
    this.state.maxScore = MAX_SCORE;
    this.state.statusText = "Waiting for the second player";

    this.onMessage("input", (client, message = {}) => {
      const player = this.state.players.get(client.sessionId);
      if (!player) {
        return;
      }

      player.inputX = clamp(Number(message.x) || 0, -1, 1);
      player.inputY = clamp(Number(message.y) || 0, -1, 1);
      player.inputSeq = Number(message.seq) || player.inputSeq;
    });

    this.onMessage("ping", (client, message = {}) => {
      client.send("pong", {
        now: Date.now(),
        echo: Number(message.now) || 0,
      });
    });

    this.setSimulationInterval((deltaTime) => {
      this.update(deltaTime / 1000);
    }, 1000 / 60);
  }

  onJoin(client, _options, auth) {
    let player = this.state.players.get(client.sessionId);
    if (!player) {
      player = new CrashBallsPlayerState();
      player.sessionId = client.sessionId;
      player.userId = auth.userId;
      player.name = auth.name || "Jugador";
      player.side = this.getOpenSide();
      this.state.players.set(client.sessionId, player);
    }

    player.connected = true;
    player.inputX = 0;
    player.inputY = 0;
    this.placePlayer(player);
    this.state.statusText = `${player.name} joined the room`;

    client.send("room_ready", {
      roomId: this.roomId,
      roomKey: this.state.roomKey,
      side: player.side,
      field: {
        width: FIELD_WIDTH,
        height: FIELD_HEIGHT,
      },
    });

    if (this.state.players.size === 2) {
      this.startMatch();
    }
  }

  async onLeave(client, consented) {
    const player = this.state.players.get(client.sessionId);
    if (!player) {
      return;
    }

    player.connected = false;
    player.inputX = 0;
    player.inputY = 0;
    this.state.phase = "paused";
    this.state.statusText = `${player.name} disconnected. Waiting for reconnection`;

    if (consented) {
      this.state.players.delete(client.sessionId);
      this.state.statusText = `${player.name} left the match`;
      return;
    }

    try {
      await this.allowReconnection(client, 20);
      const restored = this.state.players.get(client.sessionId);
      if (restored) {
        restored.connected = true;
        this.state.statusText = `${restored.name} reconnected`;
        if (this.state.players.size === 2) {
          this.state.phase = "countdown";
          this.state.resetTimer = 90;
        }
      }
    } catch (_error) {
      this.state.players.delete(client.sessionId);
      this.state.statusText = `${player.name} could not reconnect`;
    }
  }

  onDispose() {
    this.clock.clear();
  }

  getOpenSide() {
    const sides = new Set(Array.from(this.state.players.values()).map((entry) => entry.side));
    return sides.has("left") ? "right" : "left";
  }

  placePlayer(player) {
    if (player.side === "left") {
      player.x = 180;
      player.y = FIELD_HEIGHT / 2;
    } else {
      player.x = FIELD_WIDTH - 180;
      player.y = FIELD_HEIGHT / 2;
    }
    player.vx = 0;
    player.vy = 0;
  }

  startMatch() {
    this.state.phase = "countdown";
    this.state.resetTimer = COUNTDOWN_TICKS;
    this.state.statusText = "Match starting";
    this.resetPositions(0);
  }

  resetPositions(nextKickDirection) {
    for (const player of this.state.players.values()) {
      this.placePlayer(player);
    }

    this.state.ball.x = FIELD_WIDTH / 2;
    this.state.ball.y = FIELD_HEIGHT / 2;
    this.state.ball.vx = nextKickDirection * 120;
    this.state.ball.vy = 0;
  }

  update(dt) {
    this.state.tick += 1;

    if (this.state.players.size < 2) {
      this.state.phase = "waiting";
      return;
    }

    if (this.state.phase === "countdown") {
      this.state.resetTimer = Math.max(0, this.state.resetTimer - 1);
      if (this.state.resetTimer === 0) {
        this.state.phase = "playing";
        this.state.statusText = "Match in progress";
      }
      return;
    }

    if (this.state.phase !== "playing") {
      return;
    }

    for (const player of this.state.players.values()) {
      this.updatePlayer(player, dt);
    }

    this.resolvePlayerCollision();
    this.updateBall(dt);
    this.resolveBallAgainstPlayers();
    this.resolveGoals();
  }

  updatePlayer(player, dt) {
    const direction = normalizeVector(player.inputX, player.inputY);
    player.vx = clamp((player.vx + direction.x * ACCEL * dt) * PLAYER_DAMPING, -MAX_SPEED, MAX_SPEED);
    player.vy = clamp((player.vy + direction.y * ACCEL * dt) * PLAYER_DAMPING, -MAX_SPEED, MAX_SPEED);

    player.x += player.vx * dt;
    player.y += player.vy * dt;

    if (player.side === "left") {
      player.x = clamp(player.x, PLAYER_RADIUS, HALF_WIDTH - PLAYER_RADIUS);
    } else {
      player.x = clamp(player.x, HALF_WIDTH + PLAYER_RADIUS, FIELD_WIDTH - PLAYER_RADIUS);
    }
    player.y = clamp(player.y, PLAYER_RADIUS, FIELD_HEIGHT - PLAYER_RADIUS);
  }

  resolvePlayerCollision() {
    const players = Array.from(this.state.players.values());
    if (players.length < 2) {
      return;
    }

    const [left, right] = players;
    const dx = right.x - left.x;
    const dy = right.y - left.y;
    const distance = Math.hypot(dx, dy);
    const minDistance = PLAYER_RADIUS * 2;

    if (distance <= 0.0001 || distance >= minDistance) {
      return;
    }

    const nx = dx / distance;
    const ny = dy / distance;
    const overlap = (minDistance - distance) / 2;

    left.x = clamp(left.x - nx * overlap, PLAYER_RADIUS, HALF_WIDTH - PLAYER_RADIUS);
    left.y = clamp(left.y - ny * overlap, PLAYER_RADIUS, FIELD_HEIGHT - PLAYER_RADIUS);
    right.x = clamp(right.x + nx * overlap, HALF_WIDTH + PLAYER_RADIUS, FIELD_WIDTH - PLAYER_RADIUS);
    right.y = clamp(right.y + ny * overlap, PLAYER_RADIUS, FIELD_HEIGHT - PLAYER_RADIUS);
  }

  updateBall(dt) {
    const ball = this.state.ball;
    ball.x += ball.vx * dt;
    ball.y += ball.vy * dt;
    ball.vx *= BALL_DAMPING;
    ball.vy *= BALL_DAMPING;

    if (ball.y <= BALL_RADIUS) {
      ball.y = BALL_RADIUS;
      ball.vy = Math.abs(ball.vy) * 0.96;
    } else if (ball.y >= FIELD_HEIGHT - BALL_RADIUS) {
      ball.y = FIELD_HEIGHT - BALL_RADIUS;
      ball.vy = -Math.abs(ball.vy) * 0.96;
    }

    if (!isGoalY(ball.y)) {
      if (ball.x <= BALL_RADIUS) {
        ball.x = BALL_RADIUS;
        ball.vx = Math.abs(ball.vx) * 0.97;
      } else if (ball.x >= FIELD_WIDTH - BALL_RADIUS) {
        ball.x = FIELD_WIDTH - BALL_RADIUS;
        ball.vx = -Math.abs(ball.vx) * 0.97;
      }
    }
  }

  resolveBallAgainstPlayers() {
    const ball = this.state.ball;

    for (const player of this.state.players.values()) {
      const dx = ball.x - player.x;
      const dy = ball.y - player.y;
      const distance = Math.hypot(dx, dy);
      const minDistance = PLAYER_RADIUS + BALL_RADIUS;

      if (distance <= 0.0001 || distance >= minDistance) {
        continue;
      }

      const nx = dx / distance;
      const ny = dy / distance;
      const overlap = minDistance - distance;
      ball.x += nx * overlap;
      ball.y += ny * overlap;

      const playerSpeedAlongNormal = player.vx * nx + player.vy * ny;
      ball.vx += nx * (KICK_POWER + Math.max(0, playerSpeedAlongNormal) * 0.7);
      ball.vy += ny * (KICK_POWER * 0.55 + Math.abs(player.vy) * 0.35);
    }
  }

  resolveGoals() {
    const ball = this.state.ball;

    if (ball.x <= LEFT_GOAL_X && isGoalY(ball.y)) {
      this.state.scoreRight += 1;
      this.afterGoal("right");
      return;
    }

    if (ball.x >= RIGHT_GOAL_X && isGoalY(ball.y)) {
      this.state.scoreLeft += 1;
      this.afterGoal("left");
    }
  }

  afterGoal(side) {
    const leftName = this.getPlayerName("left");
    const rightName = this.getPlayerName("right");

    if (this.state.scoreLeft >= this.state.maxScore || this.state.scoreRight >= this.state.maxScore) {
      this.state.phase = "finished";
      this.state.statusText =
        this.state.scoreLeft > this.state.scoreRight
          ? `${leftName} wins`
          : `${rightName} wins`;
      this.broadcast("match_finished", {
        winnerSide: this.state.scoreLeft > this.state.scoreRight ? "left" : "right",
        scoreLeft: this.state.scoreLeft,
        scoreRight: this.state.scoreRight,
      });
      return;
    }

    this.state.phase = "countdown";
    this.state.resetTimer = COUNTDOWN_TICKS;
    this.state.statusText = side === "left" ? `${leftName} scored` : `${rightName} scored`;
    this.resetPositions(side === "left" ? 1 : -1);
  }

  getPlayerName(side) {
    for (const player of this.state.players.values()) {
      if (player.side === side) {
        return player.name;
      }
    }
    return side === "left" ? "Blue" : "Red";
  }
}

module.exports = {
  CrashBallsRoom,
  FIELD_HEIGHT,
  FIELD_WIDTH,
};

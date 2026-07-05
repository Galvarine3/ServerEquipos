const { Schema, MapSchema, defineTypes } = require("@colyseus/schema");

class CrashBallsPlayerState extends Schema {
  constructor() {
    super();
    this.sessionId = "";
    this.userId = "";
    this.name = "";
    this.side = "";
    this.connected = true;
    this.x = 0;
    this.y = 0;
    this.vx = 0;
    this.vy = 0;
    this.inputX = 0;
    this.inputY = 0;
    this.inputSeq = 0;
  }
}

defineTypes(CrashBallsPlayerState, {
  sessionId: "string",
  userId: "string",
  name: "string",
  side: "string",
  connected: "boolean",
  x: "float32",
  y: "float32",
  vx: "float32",
  vy: "float32",
  inputX: "float32",
  inputY: "float32",
  inputSeq: "float64",
});

class CrashBallsBallState extends Schema {
  constructor() {
    super();
    this.x = 0;
    this.y = 0;
    this.vx = 0;
    this.vy = 0;
  }
}

defineTypes(CrashBallsBallState, {
  x: "float32",
  y: "float32",
  vx: "float32",
  vy: "float32",
});

class CrashBallsRoomState extends Schema {
  constructor() {
    super();
    this.roomKey = "";
    this.phase = "waiting";
    this.statusText = "Waiting for players";
    this.scoreLeft = 0;
    this.scoreRight = 0;
    this.tick = 0;
    this.resetTimer = 0;
    this.maxScore = 3;
    this.players = new MapSchema();
    this.ball = new CrashBallsBallState();
  }
}

defineTypes(CrashBallsRoomState, {
  roomKey: "string",
  phase: "string",
  statusText: "string",
  scoreLeft: "uint8",
  scoreRight: "uint8",
  tick: "uint32",
  resetTimer: "uint16",
  maxScore: "uint8",
  players: { map: CrashBallsPlayerState },
  ball: CrashBallsBallState,
});

module.exports = {
  CrashBallsBallState,
  CrashBallsPlayerState,
  CrashBallsRoomState,
};

const { WebSocketServer } = require('ws');
const jwt = require('jsonwebtoken');

function initWS(server, prisma) {
  const wss = new WebSocketServer({ server, path: '/ws' });
  const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret';

  // userId -> Set<WebSocket>
  const userSockets = new Map();

  function addSocket(userId, ws) {
    if (!userSockets.has(userId)) userSockets.set(userId, new Set());
    userSockets.get(userId).add(ws);
  }

  function removeSocket(userId, ws) {
    const set = userSockets.get(userId);
    if (set) {
      set.delete(ws);
      if (set.size === 0) userSockets.delete(userId);
    }
  }

  function sendToUser(userId, data) {
    const set = userSockets.get(userId);
    if (!set) return;
    const json = JSON.stringify(data);
    for (const ws of set) {
      if (ws.readyState === ws.OPEN) {
        try { ws.send(json); } catch (_) {}
      }
    }
  }

  function broadcast(data) {
    const json = JSON.stringify(data);
    wss.clients.forEach(ws => {
      if (ws.readyState === ws.OPEN) {
        try { ws.send(json); } catch (_) {}
      }
    });
  }

  wss.on('connection', (ws, req) => {
    // Auth via header or query ?token=
    let token = null;
    const auth = req.headers['authorization'] || '';
    if (auth.startsWith('Bearer ')) token = auth.slice(7);
    if (!token) {
      const url = new URL(req.url, 'http://localhost');
      token = url.searchParams.get('token');
    }
    let userId = null;
    try {
      const payload = jwt.verify(token || '', JWT_SECRET);
      userId = payload.uid;
    } catch (_) {
      ws.close(4401, 'unauthorized');
      return;
    }

    addSocket(userId, ws);

    ws.on('message', async (raw) => {
      try {
        const msg = JSON.parse(raw.toString());
        // Handle chat send
        if (msg.type === 'message_send') {
          const { toUserId, toName, fromName, text, time, postId } = msg;
          if (!toUserId || !text) return;
          const t = BigInt(time || Date.now());
          const saved = await prisma.message.create({
            data: {
              fromUserId: userId,
              toUserId,
              fromName: fromName || '',
              toName: toName || '',
              text,
              time: t,
              postId: postId || null
            }
          });
          const payload = {
            type: 'message_new',
            data: {
              ...saved,
              // Ensure BigInt is serialized for clients
              time: typeof saved.time === 'bigint' ? Number(saved.time) : saved.time,
            },
          };
          // echo to sender and push to receiver
          sendToUser(userId, payload);
          sendToUser(toUserId, payload);
        } else if (msg.type === 'post_message_send') {
          const { postId, fromName, text, time } = msg;
          if (!postId || !text) return;
          const t = BigInt(time || Date.now());
          const saved = await prisma.postMessage.create({
            data: {
              postId,
              fromUserId: userId,
              fromName: fromName || '',
              text,
              time: t
            }
          });
          const payload = {
            type: 'post_message_new',
            data: {
              ...saved,
              time: typeof saved.time === 'bigint' ? Number(saved.time) : saved.time,
            },
          };
          // broadcast to all clients; clients filter by postId
          broadcast(payload);
        }
      } catch (_) {}
    });

    ws.on('close', () => removeSocket(userId, ws));
  });

  // Public API for routes
  return {
    postCreated: (post) => broadcast({ type: 'post_created', data: post }),
    postUpdated: (post) => broadcast({ type: 'post_updated', data: post }),
    postDeleted: (id) => broadcast({ type: 'post_deleted', data: { id } }),
    postMessageNew: (m) => broadcast({ type: 'post_message_new', data: m }),
    sendToUser,
  };
}

module.exports = { initWS };

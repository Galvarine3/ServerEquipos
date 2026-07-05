const { extractBearerToken, verifyAccessToken } = require("./game/auth");

function authMiddleware(req, res, next) {
  const h = req.headers["authorization"] || "";
  const token = extractBearerToken(h);
  if (!token) return res.status(401).json({ error: 'unauthorized' });
  try {
    const payload = verifyAccessToken(token);
    req.userId = payload.uid;
    next();
  } catch {
    res.status(401).json({ error: 'unauthorized' });
  }
}

module.exports = { authMiddleware };

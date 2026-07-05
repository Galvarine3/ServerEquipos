const jwt = require('jsonwebtoken');

function authMiddleware(req, res, next) {
  const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret';
  const h = req.headers['authorization'] || '';
  const token = h.startsWith('Bearer ') ? h.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'unauthorized' });
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.userId = payload.uid;
    next();
  } catch {
    res.status(401).json({ error: 'unauthorized' });
  }
}

module.exports = { authMiddleware };

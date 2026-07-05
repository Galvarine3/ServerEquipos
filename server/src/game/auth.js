const jwt = require("jsonwebtoken");

function extractBearerToken(headerValue = "") {
  return headerValue.startsWith("Bearer ") ? headerValue.slice(7) : null;
}

function verifyAccessToken(token) {
  if (!token) {
    throw new Error("missing_token");
  }

  const jwtSecret = process.env.JWT_SECRET || "dev_secret";
  const payload = jwt.verify(token, jwtSecret);
  if (!payload?.uid) {
    throw new Error("invalid_token");
  }

  return payload;
}

module.exports = {
  extractBearerToken,
  verifyAccessToken,
};

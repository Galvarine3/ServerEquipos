import express from 'express';
import compression from 'compression';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

app.disable('x-powered-by');
app.use(compression());
app.use(express.json());

const publicDir = path.join(__dirname, 'public');
app.use(express.static(publicDir, { maxAge: '1h', extensions: ['html'] }));

app.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

// Fallback to index.html for root
app.get('/', (_req, res) => {
  res.sendFile(path.join(publicDir, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});

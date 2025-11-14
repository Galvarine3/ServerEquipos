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

// Minimal in-memory data store for equipos
const equipos = [
  { id: 1, nombre: 'Equipo A' },
  { id: 2, nombre: 'Equipo B' }
];

app.get('/api/equipos', (_req, res) => {
  res.json(equipos);
});

app.post('/api/equipos', (req, res) => {
  const { nombre } = req.body || {};
  if (!nombre || typeof nombre !== 'string' || !nombre.trim()) {
    return res.status(400).json({ error: 'nombre requerido' });
  }
  const item = { id: Date.now(), nombre: nombre.trim() };
  equipos.push(item);
  res.status(201).json(item);
});

app.delete('/api/equipos/:id', (req, res) => {
  const id = Number(req.params.id);
  const idx = equipos.findIndex(e => e.id === id);
  if (idx === -1) {
    return res.status(404).json({ error: 'no encontrado' });
  }
  const [removed] = equipos.splice(idx, 1);
  res.json(removed);
});

// Fallback to index.html for root
app.get('/', (_req, res) => {
  res.sendFile(path.join(publicDir, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});

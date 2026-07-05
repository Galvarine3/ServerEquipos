# Equipos Backend (Node + PostgreSQL)

Express + Prisma + JWT. Pensado para usarse con la APK.

## Requisitos
- Node 20+
- PostgreSQL

## Variables de entorno
Copia `.env.example` a `.env` y completa:
- `DATABASE_URL` URL de Postgres
- `JWT_SECRET` secreto fuerte
- `PORT` puerto del API principal
- `GAME_PORT` puerto del servidor Colyseus de `Crash Balls`
- `GAME_BRIDGE_PORT` puerto del WebSocket JSON para Android
- `GOOGLE_CLIENT_ID` Client ID para login Google
- `APP_BASE_URL` URL base para links de verificacion

## Scripts
- `npm run dev` desarrollo del API principal
- `npm run start` produccion del API principal
- `npm run game:dev` desarrollo del servidor Colyseus
- `npm run game:start` produccion del servidor Colyseus
- `npm run prisma:generate` generar cliente Prisma
- `npm run prisma:migrate` aplicar migraciones

## API principal
- Archivo de entrada: `src/app.js`
- WebSocket actual de comunidad/chat: `src/ws.js`
- Endpoints principales:
  `POST /auth/register`
  `POST /auth/login`
  `POST /auth/google`
  `POST /auth/refresh`
  `GET /players`
  `GET /matches`

## Crash Balls realtime
- Archivo de entrada: `src/game-server.js`
- Framework: Colyseus
- Sala base: `crash-balls`
- Matchmaking: filtro por `roomKey`
- Autenticacion: reutiliza el mismo `Bearer` JWT del backend actual
- Reconexion: la sala permite reconectar durante 20 segundos
- Puente Android: WebSocket JSON en `/crash-balls-bridge`

## Estado de la migracion
- Ya existe una base de servidor autoritativo con fisica simple, marcador, countdown y reconexion.
- Ya existe un puente WebSocket JSON para que Android no tenga que hablar el protocolo binario de Colyseus.
- Falta conectar `MainActivity.kt` a este puente nuevo y retirar el flujo peer-to-peer actual.

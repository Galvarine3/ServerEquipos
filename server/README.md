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

## Estado de la migracion
- Ya existe una base de servidor autoritativo con fisica simple, marcador, countdown y reconexion.
- Falta migrar el cliente Android al protocolo de Colyseus o agregar un puente compatible con Kotlin.
- La documentacion oficial de Colyseus no ofrece hoy un SDK nativo oficial para Android/Kotlin, asi que esa integracion requiere una decision aparte antes de retirar el flujo actual.

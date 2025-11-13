# Equipos Backend (Node + PostgreSQL)

Express + Prisma + JWT. Pensado para usarse con la APK (la webapp sigue local).

## Requisitos
- Node 18+
- PostgreSQL (Render u otro proveedor)

## Variables de Entorno
Copia `.env.example` a `.env` y completa:
- `DATABASE_URL` URL de Postgres
- `JWT_SECRET` secreto fuerte
- `PORT` (opcional, Render lo inyecta)

## Scripts
- `npm run dev` desarrollo (nodemon)
- `npm run start` producción
- `npm run prisma:generate` generar cliente Prisma
- `npm run prisma:migrate` aplicar migraciones en producción

## Despliegue en Render
1. Sube esta carpeta `server/` como repo en GitHub (o raíz del repo).
2. Crea un Web Service en Render apuntando a ese repo.
3. Build Command:
```
npm ci && npx prisma generate && npm run prisma:migrate
```
4. Start Command:
```
npm run start
```
5. Configura variables: `DATABASE_URL`, `JWT_SECRET`.
6. (Opcional) Crea una Base de Datos PostgreSQL en Render y usa su `DATABASE_URL`.

## Endpoints
- `POST /auth/register` { email, password }
- `POST /auth/login` { email, password }
- `POST /auth/refresh` { refreshToken }
- `GET /players` (Auth)
- `POST /players` (Auth)
- `PUT /players/:id` (Auth)
- `DELETE /players/:id` (Auth)
- `POST /players/bulk` (Auth, upsert por nombre)
- `GET /matches` (Auth)
- `POST /matches` (Auth)
- `PUT /matches/:id` (Auth)
- `DELETE /matches/:id` (Auth)

## Notas
- Los datos se aíslan por usuario (`userId`).
- `players.userId + name` es único para facilitar upsert.
- `teamA`/`teamB` son JSON (puedes guardar arrays de objetos con `name`, `isGoalkeeper`, etc.).

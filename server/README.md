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
- `GOOGLE_CLIENT_ID` (opcional) Client ID para login Google (audience del ID token)
- `APP_BASE_URL` (opcional) URL base para links de verificación (por defecto `http://localhost:3000`)
- `RESEND_API_KEY` (obligatoria para registro por email) API key de Resend
- `MAIL_FROM` (obligatoria para registro por email) dirección remitente autorizada en Resend; para pruebas puede ser `onboarding@resend.dev`

El registro por email crea una cuenta no verificada y envía un enlace válido durante 24 horas. El login se rechaza hasta que el enlace sea abierto. En producción, `APP_BASE_URL` debe ser la URL pública del backend para que el enlace recibido funcione.

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
- `POST /auth/register` { email, password, name }
- `POST /auth/login` { email, password }
- `POST /auth/google` { idToken }
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

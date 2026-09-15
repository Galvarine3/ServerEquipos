-- Recuperacion de contrasena por codigo enviado al correo.
ALTER TABLE "User" ADD COLUMN "resetCodeHash" TEXT;
ALTER TABLE "User" ADD COLUMN "resetCodeExpiresAt" TIMESTAMP(3);
ALTER TABLE "User" ADD COLUMN "resetCodeAttempts" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "User" ADD COLUMN "resetCodeSentAt" TIMESTAMP(3);

-- Invalidacion de sesiones al cambiar la contrasena.
ALTER TABLE "User" ADD COLUMN "passwordChangedAt" TIMESTAMP(3);

-- Add Google auth fields to User
ALTER TABLE "User" ALTER COLUMN "passwordHash" DROP NOT NULL;

ALTER TABLE "User" ADD COLUMN "googleSub" TEXT;

-- Unique Google subject (multiple NULLs are allowed in PostgreSQL)
CREATE UNIQUE INDEX "User_googleSub_key" ON "User"("googleSub");

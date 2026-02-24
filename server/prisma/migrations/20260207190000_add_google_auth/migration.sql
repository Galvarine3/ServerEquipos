-- Add google auth fields
ALTER TABLE "User"
  ADD COLUMN "googleSub" TEXT,
  ALTER COLUMN "passwordHash" DROP NOT NULL;

CREATE UNIQUE INDEX "User_googleSub_key" ON "User"("googleSub");

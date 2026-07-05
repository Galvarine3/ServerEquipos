-- Add email verification fields to User
ALTER TABLE "User"
  ADD COLUMN "emailVerified" BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN "verificationToken" TEXT,
  ADD COLUMN "verificationSentAt" TIMESTAMP(3);

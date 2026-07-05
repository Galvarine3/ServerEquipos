-- AlterTable
ALTER TABLE "Message" ADD COLUMN     "threadId" TEXT;

-- CreateIndex
CREATE INDEX "Message_threadId_fromUserId_time_idx" ON "Message"("threadId", "fromUserId", "time");

-- CreateIndex
CREATE INDEX "Message_threadId_toUserId_time_idx" ON "Message"("threadId", "toUserId", "time");

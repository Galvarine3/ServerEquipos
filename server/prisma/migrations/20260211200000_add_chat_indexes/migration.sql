-- Add chat indexes
CREATE INDEX "CommunityPost_time_idx" ON "CommunityPost"("time");

CREATE INDEX "Message_postId_fromUserId_time_idx" ON "Message"("postId", "fromUserId", "time");
CREATE INDEX "Message_postId_time_idx" ON "Message"("postId", "time");
CREATE INDEX "Message_postId_toUserId_time_idx" ON "Message"("postId", "toUserId", "time");

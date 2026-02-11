-- Speed up common community/chat queries

-- Community posts are fetched ordered by time desc
CREATE INDEX IF NOT EXISTS "CommunityPost_time_idx" ON "CommunityPost" ("time" DESC);

-- Direct messages are filtered by postId and user ids, ordered by time
CREATE INDEX IF NOT EXISTS "Message_postId_time_idx" ON "Message" ("postId", "time");
CREATE INDEX IF NOT EXISTS "Message_postId_fromUserId_time_idx" ON "Message" ("postId", "fromUserId", "time");
CREATE INDEX IF NOT EXISTS "Message_postId_toUserId_time_idx" ON "Message" ("postId", "toUserId", "time");

-- Group messages (table might not exist on all DBs depending on migration history)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'PostMessage'
  ) THEN
    EXECUTE 'CREATE INDEX IF NOT EXISTS "PostMessage_postId_time_idx" ON "PostMessage" ("postId", "time")';
  END IF;
END $$;


CREATE TABLE already_done (
  ad_idempotency_key TEXT PRIMARY KEY,
  at_t_done_epoch_s BIGINT
);


CREATE TABLE processed_comments (
  reddit_comment_id TEXT PRIMARY KEY,
  t_processed_epoch_s BIGINT
);




CREATE TABLE pat_comment_requests (
  reddit_parent_id TEXT,
  reddit_user_fullname TEXT,
  pat_request_epoch_s BIGINT,

  reddit_user_name TEXT,

  pat_sent_reminder BOOLEAN NOT NULL DEFAULT FALSE,
  pat_subreddit_id TEXT,

  pat_meta JSONB,

  PRIMARY KEY(reddit_parent_id, reddit_user_fullname)
);

CREATE INDEX pat_sent_reminder__index ON pat_comment_requests (pat_sent_reminder);
CREATE INDEX pat_comment_requests_by_time ON pat_comment_requests (pat_request_epoch_s);



CREATE TABLE pat_subreddit_checkpoints (
  pat_subreddit_id TEXT PRIMARY KEY,
  pat_checked_until_epoch_s BIGINT
);

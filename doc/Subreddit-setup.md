# Subreddit setup

The moderated subreddits should allow cross-posting, have u/PatientModBot as a moderator,
and an Automod configuration such as:


```yaml

type: comment # Removes all new comments, except those by AutoModerator.
moderators_exempt: true
author:
    ~name: [AutoModerator]
is_edited: false
action: remove
---

type: submission # Posts an explicative comment on each new submission.
moderators_exempt: false
is_edited: false
comment_stickied: true
comment: |
    Markdown **text** explaining how to post on r/{{subreddit}}

    blah blah blah

```

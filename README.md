Code for some Reddit bots I maintain (only 1 at the time of writing).

# `reddit-bots.patience`

This is a Reddit bot for moderating subreddits in such a way as to apply a special rule: enforcing that **_immediate replies are impossible. One day has to pass between reading some content and commenting on it._**

This is achieved by a combination of Reddit AutoModerator (see the configuration [here](./doc/Subreddit-setup.md)), and a watchful bot that will scrape new Reddit comments, send reminders and messages, mirror other subreddits through automated cross-posting, etc.

This bot is programmed in the [Clojure](https://clojure.org/) language. The starting point for running it is [`reddit-bots.patience.main`](./src/reddit_bots/patience/main.clj).

See also the [doc/](./doc/) folder.

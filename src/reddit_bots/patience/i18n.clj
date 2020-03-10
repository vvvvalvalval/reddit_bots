(ns reddit-bots.patience.i18n)

(defmulti pat-wording (fn [reddit-sub wording-key & _args]
                        [wording-key
                         (:pat_subreddit_locale reddit-sub)]))

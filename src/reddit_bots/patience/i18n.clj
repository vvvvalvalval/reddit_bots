(ns reddit-bots.patience.i18n)

(defmulti pat-wording (fn [reddit-sub wording-key & _args]
                        [wording-key
                         (:pat_subreddit_locale reddit-sub)]))



(defmethod pat-wording [:pat-user-handle :locale/fr]
  [_reddit-sub _wk user-name]
  (if (nil? user-name)
    "(utilisateur supprimé)"
    (str "u/" user-name)))

(defmethod pat-wording [:pat-user-handle :locale/en]
  [_reddit-sub _wk user-name]
  (if (nil? user-name)
    "(deleted user)"
    (str "u/" user-name)))
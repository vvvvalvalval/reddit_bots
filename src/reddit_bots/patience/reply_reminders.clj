(ns reddit-bots.patience.reply-reminders
  (:require [reddit-bots.patience.i18n :as i18n]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [reddit-bots.patience.pushshift :as ps]
            [reddit-bots.patience.reddit :as reddit]
            [taoensso.timbre :as log]
            [reddit-bots.patience.utils :as u]
            [reddit-bots.patience.env.sql :as sql]
            [reddit-bots.patience.env :as patenv]
            [reddit-bots.patience.env.effect :as pat-eff]
            [reddit-bots.patience.env.reddit :as redd]))


(defmethod i18n/pat-wording [:pat-24h-reminder--subject :locale/fr]
  [reddit-sub _lang to-notify parent-cmt]
  (format "r/%s : tu peux désormais répondre à %s"
    (:pat_subreddit_id reddit-sub)
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))

(defmethod i18n/pat-wording [:pat-24h-reminder--subject :locale/en]
  [reddit-sub _lang to-notify parent-cmt]
  (format "r/%s : you may now reply to %s"
    (:pat_subreddit_id reddit-sub)
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))


(defmethod i18n/pat-wording [:pat-24h-reminder--body :locale/fr]
  [reddit-sub _lang to-notify parent-cmt]
  (format
    "Bonjour, ceci est un message automatique de modération de r/%s.

 Le délai de 24h étant écoulé, **tu peux désormais répondre [%s](%s)** de %s, comme prévu par les [règles du forum](%s).

 Pour ce faire, rédige et publie ta réponse **dans un nouveau commentaire** [au même endroit](%s). (**Attention:** il s'agit bien de publier un _nouveau_ commentaire, PAS de modifier ton pré-commentaire.)

 Prends tout le temps qu'il te faudra; si tu décides finalement de ne pas répondre, ce n'est pas un problème."
    (:pat_subreddit_id reddit-sub)
    (if (str/starts-with? (:reddit_parent_id to-notify) "t3_")
      "à la publication"
      "au commentaire")
    (str "https://reddit.com" (:permalink parent-cmt))
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))
    (:pat_subreddit_rules_url reddit-sub)
    (str "https://reddit.com" (:permalink parent-cmt))))

(defmethod i18n/pat-wording [:pat-24h-reminder--body :locale/en]
  [reddit-sub _lang to-notify parent-cmt]
  (format
    "Hi, this is an automated moderation mail from r/%s.

 The 24h delay is up: **you may now reply to [this %s](%s)** by %s, as per the [rules of this forum](%s).

 To do so, write and post your reply **as a new comment** at [the same location](%s). (**Caution:** you have to post a _new_ comment, NOT update your pre-comment.)

 Take all the time you need. If on second thought you decide not to reply, that's fine."
    (:pat_subreddit_id reddit-sub)
    (if (str/starts-with? (:reddit_parent_id to-notify) "t3_")
      "post"
      "comment")
    (str "https://reddit.com" (:permalink parent-cmt))
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))
    (:pat_subreddit_rules_url reddit-sub)
    (str "https://reddit.com" (:permalink parent-cmt))))



(defn reminder-commands
  [pat-env reddit-subs]
  (let [now-epoch-s (u/date-to-epoch-s (::patenv/now-date pat-env))
        id->reddit-sub (u/index-by :pat_subreddit_id reddit-subs)
        delay-s (* 60 60 24)
        epoch-24h-ago-s (-> now-epoch-s
                          (- delay-s))
        to-notify
        (sql/query (::patenv/sql-client pat-env)
          ["SELECT reddit_parent_id, reddit_user_fullname, reddit_user_name, pat_subreddit_id
            FROM pat_comment_requests
            WHERE NOT pat_sent_reminder
            AND pat_request_epoch_s < ?"
           epoch-24h-ago-s]
          {})]
    (->> to-notify
      (mapv
        (fn [to-notify]
          (let [reddit-sub (get id->reddit-sub (:pat_subreddit_id to-notify))
                parent-cmt (-> (redd/fetch-thing-by-fullname (::patenv/reddit-client pat-env)
                                 (:reddit_parent_id to-notify))
                             (select-keys
                               [:user_removed
                                :author
                                :permalink]))
                reddit-notif-req
                {:method :post
                 :reddit.api/path "/api/mod/conversations"
                 :form-params {:isAuthorHidden false
                               :srName (:pat_subreddit_id reddit-sub)
                               :subject (i18n/pat-wording reddit-sub :pat-24h-reminder--subject
                                          to-notify parent-cmt)
                               :body (i18n/pat-wording reddit-sub :pat-24h-reminder--body
                                       to-notify parent-cmt)
                               :to (:reddit_user_name to-notify)}}
                db-update
                ["UPDATE pat_comment_requests
                     SET pat_sent_reminder = TRUE
                     WHERE reddit_parent_id = ? AND reddit_user_fullname = ?"
                 (:reddit_parent_id to-notify)
                 (:reddit_user_fullname to-notify)]]
            [{::pat-eff/effect-type ::pat-eff/change-reddit!
              ::pat-eff/reddit-req reddit-notif-req}
             {::pat-eff/effect-type ::pat-eff/jdbc-execute!
              ::pat-eff/jdbc-args [db-update {}]}]))))))


(defn send-reminders!
  [pat-env reddit-subs]
  (log/debug "Sending reminders...")
  (let [reminder-cmds (reminder-commands pat-env reddit-subs)]
    (log/debug (count reminder-cmds) "reminders to send...")
    (run! pat-eff/effect! reminder-cmds)))


(comment
  (jdbc/execute! pg-db
    ["UPDATE pat_comment_requests SET pat_sent_reminder = FALSE WHERE TRUE"])

  (reminder-commands
    pg-db
    ["discussion_patiente"])

  *e

  (send-reminders!
    pg-db
    reddit-creds
    ["discussion_patiente"])

  (def rc1 (assoc reddit-creds
             :username "vvvvalvalval"
             :password ""))



  (reddit/reddit-request reddit-creds
    {:method :post
     :reddit.api/path "/api/mod/conversations"
     :form-params {:api_type "json"
                   :isAuthorHidden false
                   :srName "discussion_patiente"
                   :subject "Test message 0"
                   :body "Do you copy ? 1 2 1 2 1 2"
                   :to "u/vvvvalvalval"}})


  (reddit/reddit-request reddit-creds
    {:method :post
     :reddit.api/path "/api/compose"
     :form-params {:api_type "json"
                   :from_sr "discussion_patiente"
                   :subject "Test message 1"
                   :text "Do you copy ?"
                   :to "vvvvalvalval"}})

  (reddit/reddit-request rc1
    {:method :post
     :reddit.api/path "/api/mod/conversations"
     :form-params {:api_type "json"
                   :isAuthorHidden false
                   :srName "discussion_patiente"
                   :subject "Test message 9"
                   :body "Do you copy u/PatientModBot ?"
                   :to "PatientModBot"}})


  (reddit/reddit-request rc1
    {:method :post
     :reddit.api/path "/api/compose"
     :form-params {:api_type "json"
                   :from_sr "discussion_patiente"
                   :subject "Test message 7"
                   :text "Do you copy u/PatientModBot ?"
                   :to "PatientModBot"}})

  *e)


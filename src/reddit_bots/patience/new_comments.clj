(ns reddit-bots.patience.new-comments
  (:require [reddit-bots.patience.i18n :as i18n]
            [clojure.string :as str]
            [reddit-bots.patience.utils :as u]
            [reddit-bots.patience.pushshift :as ps]
            [clojure.java.jdbc :as jdbc]
            [reddit-bots.patience.reddit :as reddit]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [vvvvalvalval.supdate.api :as supd]))



(defmethod i18n/pat-wording [:pat-first-notification--subject :locale/fr]
  [reddit-sub _wk parent-cmt]
  (format "r/%s : tu pourras répondre à %s dans 24h"
    (:pat_subreddit_id reddit-sub)
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))

(defmethod i18n/pat-wording [:pat-first-notification--subject :locale/en]
  [reddit-sub _wk parent-cmt]
  (format "r/%s : you can reply to %s in 24h"
    (:pat_subreddit_id reddit-sub)
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))


(defmethod i18n/pat-wording [:pat-first-notification--body :locale/fr]
  [reddit-sub _wk cmt parent-cmt]
  (format
    "Bonjour, ceci est un message automatique de modération de r/%s.
 Comme prévu dans le fonctionnement du forum, tu as manifesté ton intention de répondre à [%s](%s) de %s, en publiant un 'pré-commentaire',
 ; tu pourras publier ta véritable réponse dans **environ 24h**
 (tu recevras un message de rappel).

 Tu peux profiter de ces 24h pour réfléchir posément à ce que tu vas écrire (la nuit porte conseil).

 Ton pré-commentaire n'est visible que par toi; je te **conseille de le supprimer** dès maintenant pour ne pas encombrer ta discussion."
    (:pat_subreddit_id reddit-sub)
    (if (str/starts-with? (:reddit_parent_id cmt) "t3_")
      "cette publication"
      "ce commentaire")
    (str "https://reddit.com" (:permalink parent-cmt))
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))

(defmethod i18n/pat-wording [:pat-first-notification--body :locale/en]
  [reddit-sub _wk cmt parent-cmt]
  (format
    "Hi, this is an automated moderation mail from r/%s.
 As planned by the forum rules, you declared your intention of replying to this [%s](%s) by %s,
 by posting a 'pre-comment'. (This pre-comment is visible only to you; **I recommend you delete it** right now to get a cleaner view of the discussion).

 You will be able to post your actual reply in **approximately 24 hours** - I will send you a reminder.
 You can use this delay to craft a thoughtful answer (tip: sleeping always helps).

 It's perfectly fine if you need more than 24h to design your reply - take your time.
 If you eventually decide not to reply at all, that's fine as well."
    (:pat_subreddit_id reddit-sub)
    (if (str/starts-with? (:reddit_parent_id cmt) "t3_")
      "post"
      "comment")
    (str "https://reddit.com" (:permalink parent-cmt))
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))



(defn pre-comment-commands
  [reddit-creds reddit-sub cmt]
  (let [{user-fullname :reddit_user_fullname parent-id :reddit_parent_id} cmt
        write-comment-request-row
        {:reddit_parent_id parent-id
         :reddit_user_fullname user-fullname
         :pat_request_epoch_s (u/date-to-epoch-s (:reddit.comment/created_utc cmt))
         :reddit_user_name (:reddit_user_name cmt)
         :pat_sent_reminder false
         :pat_subreddit_id (:pat_subreddit_id reddit-sub)}
        send-notif-reddit-req
        (let [parent-cmt
              (-> (reddit/fetch-thing-by-fullname reddit-creds
                    (:reddit_parent_id cmt))
                (select-keys
                  [:user_removed
                   :author
                   :permalink]))]
          {:method :post
           :reddit.api/path "/api/mod/conversations"
           ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
           :form-params {:isAuthorHidden false
                         :srName (:pat_subreddit_id reddit-sub)
                         :subject (i18n/pat-wording reddit-sub :pat-first-notification--subject
                                    parent-cmt)
                         :body (i18n/pat-wording reddit-sub :pat-first-notification--body
                                 cmt parent-cmt)
                         :to (:reddit_user_name cmt)}})]
    [write-comment-request-row
     send-notif-reddit-req]))

(comment
  (def reddit-sub-Clojure
    {:pat_subreddit_id "Clojure"
     :pat_subreddit_locale :locale/en})

  (pre-comment-commands reddit-sub-Clojure cmt)

  (def rc1
    (assoc reddit-creds
      :reddit/username "vvvvalvalval"
      :reddit/password ""))

  (reddit/reddit-request rc1
    {:method :post
     :reddit.api/path "/api/remove"
     :form-params
     {:id (str "t1_" "fk3st7p")
      :spam false}})

  (reddit/reddit-request rc1
    {:method :get
     :reddit.api/path "/api/info"
     :query-params {"id" (str "t1_" "fk3st7p")}})


  (reddit/reddit-request reddit-creds
    {:method :post
     :reddit.api/path "/api/del"
     :form-params
     {:id (str "t1_" "fk3st7p")}})

  *e)


(defmethod i18n/pat-wording [:pat-too-early-notification--subject :locale/fr]
  [reddit-sub _lang parent-cmt]
  (format "r/%s : tu as répondu trop tôt à %s"
    (:pat_subreddit_id reddit-sub)
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))

(defmethod i18n/pat-wording [:pat-too-early-notification--subject :locale/en]
  [reddit-sub _lang parent-cmt]
  (format "r/%s : your reply to %s was premature"
    (:pat_subreddit_id reddit-sub)
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))


(defmethod i18n/pat-wording [:pat-too-early-notification--body :locale/fr]
  [reddit-sub _lang cmt parent-cmt]
  (format
    "Bonjour, ceci est un message automatique de modération de r/%s.

 Tu as répondu à [%s](%s) de %s sans respecter le délai minimum de 24h;
 par conséquent, **ta réponse a été supprimée**.

 Tu recevras un message de rappel une fois le délai écoulé: tu pourras publier
 ta réponse à partir de ce moment là."
    (:pat_subreddit_id reddit-sub)
    (if (str/starts-with? (:reddit_parent_id cmt) "t3_")
      "cette publication"
      "ce commentaire")
    (str "https://reddit.com" (:permalink parent-cmt))
    (:author parent-cmt "(utilisateur supprimé)")))

(defmethod i18n/pat-wording [:pat-too-early-notification--body :locale/en]
  [reddit-sub _lang cmt parent-cmt]
  (format
    "Hi, this is an automated moderation mail from r/%s.

 Your reply to this [%s](%s) by %s did not abide by the minimum delay of 24h;
 as a result, **your comment was deleted**.

 You will receive a reminder when the time is up, at which point you will be able to post your reply."
    (:pat_subreddit_id reddit-sub)
    (if (str/starts-with? (:reddit_parent_id cmt) "t3_")
      "post"
      "comment")
    (str "https://reddit.com" (:permalink parent-cmt))
    (i18n/pat-wording reddit-sub :pat-user-handle
      (:author parent-cmt))))


(defn too-early-commands
  [reddit-creds reddit-sub cmt]
  (let [send-notif-reddit-req
        (let [parent-cmt (-> (reddit/fetch-thing-by-fullname reddit-creds
                               (:reddit_parent_id cmt))
                           (select-keys
                             [:user_removed
                              :author
                              :permalink]))]
          {:method :post
           :reddit.api/path "/api/mod/conversations"
           ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
           :form-params {:isAuthorHidden false
                         :srName (:pat_subreddit_id reddit-sub)
                         :subject (i18n/pat-wording reddit-sub :pat-too-early-notification--subject
                                    parent-cmt)
                         :body (i18n/pat-wording reddit-sub :pat-too-early-notification--body
                                 cmt parent-cmt)
                         :to (:reddit_user_name cmt)}})]
    [send-notif-reddit-req]))


(defn should-bypass-processing?
  [reddit-sub cmt]
  ;; HACK (Val, 11 Mar 2020)
  (= "PatientModBot"
    (:reddit_user_fullname cmt)))

(defn process-new-comment!
  [pg-db reddit-creds reddit-sub cmt]
  (when-not (should-bypass-processing? reddit-sub cmt)
    (let [{user-fullname :reddit_user_fullname parent-id :reddit_parent_id} cmt]
      (when (and (some? user-fullname) (some? parent-id))
        (let [[[pat_request_epoch_s]]
              (rest
                (jdbc/query pg-db
                  ["SELECT pat_request_epoch_s FROM pat_comment_requests
               WHERE reddit_user_fullname = ? and reddit_parent_id = ?"
                   user-fullname parent-id]
                  {:as-arrays? true}))
              pre-comment? (nil? pat_request_epoch_s)]
          (if pre-comment?
            (let [[write-comment-request-row
                   send-notif-reddit-req]
                  (pre-comment-commands reddit-creds reddit-sub cmt)]
              (jdbc/insert! pg-db "pat_comment_requests"
                write-comment-request-row)
              (reddit/reddit-request reddit-creds
                send-notif-reddit-req))
            (let [too-early? (>
                               (* 24 60 60)
                               (-
                                 (u/date-to-epoch-s (:reddit.comment/created_utc cmt))
                                 pat_request_epoch_s))]
              (if too-early?
                (let [[send-notif-reddit-req]
                      (too-early-commands reddit-creds reddit-sub cmt)]
                  (reddit/reddit-request reddit-creds
                    send-notif-reddit-req))
                (let [approve-req
                      {:method :post
                       :reddit.api/path "/api/approve"
                       :form-params {:id (str "t1_" (:reddit_comment_id cmt))}}]
                  (reddit/reddit-request reddit-creds approve-req))))))))))


(def sql_unprocessed-comments
  "
SELECT reddit_comment_id
FROM jsonb_array_elements_text(?::jsonb) AS input(reddit_comment_id)
WHERE NOT EXISTS (
  SELECT 1 FROM processed_comments AS pc
  WHERE pc.reddit_comment_id = input.reddit_comment_id
)")

(defn process-new-comments!
  [pg-db reddit-creds reddit-sub cmts]
  (let [unprocessed-ids
        (into #{}
          (map :reddit_comment_id)
          (jdbc/query pg-db
            [sql_unprocessed-comments
             (json/generate-string
               (mapv :reddit_comment_id
                 cmts))]))]
    (log/debug (format "In r/%s," (:pat_subreddit_id reddit-sub)) (count cmts) "comments," (count unprocessed-ids) "unprocessed...")
    (->> cmts
      (filter #(contains? unprocessed-ids (:reddit_comment_id %)))
      (run!
        (fn [cmt]
          (try
            (process-new-comment! pg-db reddit-creds reddit-sub
              cmt)
            (jdbc/insert! pg-db "processed_comments"
              (merge
                (select-keys cmt [:reddit_comment_id])
                {:t_processed_epoch_s (u/date-to-epoch-s (java.util.Date.))}))
            (catch Throwable err
              (log/error err "Error processing comment."))))))))


(comment
  (def reddit-sub {:pat_subreddit_id "Clojure"
                   :pat_subreddit_locale :locale/en})

  (;process-new-comments! pg-db reddit-creds reddit-sub
    (ps/fetch-comments-in-range
      "Clojure"
      (-> #inst"2020-02-25T20:38:14.000-00:00" .getTime)
      (-> #inst"2020-02-25T23:16:28.000-00:00" .getTime)))

  *e)

(defn set-subreddit-checkpoint!
  [pg-db pat_subreddit_id checked-until-epoch-s]
  (jdbc/execute! pg-db
    ["INSERT INTO pat_subreddit_checkpoints(pat_subreddit_id, pat_checked_until_epoch_s)
       VALUES (?, ?)
       ON CONFLICT (pat_subreddit_id) DO UPDATE SET
       pat_checked_until_epoch_s = ?"
     pat_subreddit_id checked-until-epoch-s checked-until-epoch-s]))


(defn fetch-recent-comments-in-sub
  [reddit-creds pat_subreddit_id from-epoch-s]
  (into []
    (comp
      (map (fn reshaped-cmt [m]
             (-> m
               :data
               (supd/supdate
                 {:created_utc u/epoch-s-to-date})
               (u/rename-keys
                 {:user_removed :reddit.comment/user_removed
                  :author :reddit_user_name
                  :author_fullname :reddit_user_fullname
                  :parent_id :reddit_parent_id
                  :id :reddit_comment_id
                  :created_utc :reddit.comment/created_utc}))))
      (filter (fn recent-comment? [cmt]
                (-> cmt :reddit.comment/created_utc u/date-to-epoch-s
                  (>= from-epoch-s)))))
    (reddit/request-lazy-listing reddit-creds
      ;; NOTE this API endpoint is not officially documented (Val, 11 Mar 2020)
      ;; I found about it here: https://www.reddit.com/r/modhelp/comments/btji32/want_to_see_unmoderated_comments/
      {:method :get
       :reddit.api/path (format "/r/%s/comments" pat_subreddit_id)
       :query-params {:api_type :json
                      :limit 25}})))

(comment
  (fetch-recent-comments-in-sub reddit-creds "discussion_patiente"
    (-> (u/now-date) u/date-to-epoch-s (- (* 60 60 24))))


  *e)


(def reddit-new-comments-lag
  "The 'backard safety margin' we take for fetching new Reddit comments."
  (* 60 5))


(defn process-new-recent-comments-in-sub!
  [pg-db reddit-creds reddit-sub]
  (let [start-time
        (->
          (or
            (->
              (jdbc/query pg-db
                ["SELECT pat_checked_until_epoch_s FROM pat_subreddit_checkpoints
           WHERE pat_subreddit_id = ?"
                 (:pat_subreddit_id reddit-sub)]
                {:as-arrays? true})
              rest
              ffirst)
            (-> (u/now-date) u/date-to-epoch-s))
          (- reddit-new-comments-lag))
        end-time (-> (java.util.Date.) .getTime (quot 1000))
        recent-comments (fetch-recent-comments-in-sub reddit-creds (:pat_subreddit_id reddit-sub) start-time)]
    (process-new-comments! pg-db reddit-creds reddit-sub
      recent-comments)
    (set-subreddit-checkpoint! pg-db (:pat_subreddit_id reddit-sub) end-time)))

(defn process-new-recent-comments!
  [pg-db reddit-creds reddit-subs]
  (log/debug "Processing new recent comments")
  (run!
    #(process-new-recent-comments-in-sub! pg-db reddit-creds %)
    reddit-subs))

(comment
  (process-new-recent-comments! pg-db reddit-creds ["discussion_patiente"])


  (jdbc/query pg-db ["SELECT * FROM processed_comments"])
  (->
    (jdbc/query pg-db ["SELECT * FROM pat_subreddit_checkpoints"])
    (supd/supdate [{:pat_checked_until_epoch_s u/epoch-s-to-date}]))
  (jdbc/query pg-db ["SELECT * FROM pat_comment_requests"])

  (jdbc/update! pg-db "pat_comment_requests"
    {:pat_request_epoch_s (- 1583845323 (* 60 60 24))}
    ["reddit_parent_id = ? AND reddit_user_fullname = ?"

     "t3_fg2djf",
     "t2_4tf84r5k"])

  ;; Cheating with the clock
  (jdbc/execute! pg-db
    ["UPDATE pat_comment_requests
     SET pat_request_epoch_s = pat_request_epoch_s - (60 * 60 * 24)
     WHERE reddit_parent_id = ? AND reddit_user_fullname = ?"
     "t1_fjanj6u",
     "t2_4tf84r5k"])

  (jdbc/execute! pg-db ["TRUNCATE pat_subreddit_checkpoints"])

  (jdbc/execute! pg-db
    ["DELETE FROM processed_comments WHERE reddit_comment_id = ?"
     "fitl8es"])

  (jdbc/execute! pg-db
    ["DELETE FROM pat_comment_requests
    WHERE reddit_parent_id = ? AND reddit_user_fullname = ?"
     "t1_fitl83r" "t2_q7fwt"])


  #inst"2020-03-02T20:04:10.056-00:00"
  (ps/find-post-or-comment-by-fullname "t1_fjaqdmj" nil)
  (ps/find-post-or-comment-by-fullname "t1_fitl83r" nil)
  (ps/find-post-or-comment-by-fullname "t3_f9t460" [:user_removed
                                                    :author
                                                    :permalink])

  (u/to-sorted-map
    (reddit/fetch-thing-by-fullname reddit-creds
      "t3_f9t460"))


  (set-subreddit-checkpoint! pg-db "discussion_patiente"
    (-> #inst "2020-01-01" .getTime (quot 1000)))


  (ps/fetch-comments-in-range "discussion_patiente"
    [:retrieved_on :created_utc]
    (-> #inst "2020-03-04" .getTime)
    (-> (java.util.Date.) .getTime))
  *e)


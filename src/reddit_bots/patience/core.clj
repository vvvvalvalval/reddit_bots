(ns reddit-bots.patience.core
  (:require [clj-http.client :as htc]
            [clj-http.util :as htu]
            [clojure.string :as str]
            [cheshire.core :as json]
            [com.rpl.specter :as sp]
            [vvvvalvalval.supdate.api :as supd]
            [clojure.set :as cset]
            [reddit-bots.patience.utils :as u]
            [clojure.java.jdbc :as jdbc]
            [reddit-bots.patience.pushshift :as ps]
            [reddit-bots.patience.reddit :as reddit]))

;; App credentials: https://www.reddit.com/prefs/apps

;; Response format: https://github.com/reddit-archive/reddit/wiki/JSON

(comment
  (require 'sc.api)

  *e)

(comment
  ;; go to
  reddit-bots.patience.db.sql
  (def pg-db *1)

  *e)

(defn gen-token [l]
  (apply str
    (repeatedly l
      #(rand-nth "azertyuiopqsdfghjklmwxcvbnAZERTYUIOPQSDFGHJKLMWXCVBN1234567890"))))


(declare reddit-state)

(comment

  (def reddit-state
    (gen-token 16)))



(declare reddit-username reddit-pwd)

(do
  (def reddit-client-id "Y9V30XN7RC-2EQ")
  (def reddit-username "PatientModBot"))

(def user-agent
  (str "server:" reddit-client-id ":0.1" " (by /u/vvvvalvalval)"))

(declare reddit-creds reddit-client-secret)

(comment
  (def reddit-client-secret "")
  (def reddit-pwd "")

  (def reddit-creds
    {:reddit/username reddit-username
     :reddit/client-id reddit-client-id
     :reddit/client-secret reddit-client-secret
     :reddit/password reddit-pwd}))

(def redirect-uri "http://localhost:8080/redirect")

(comment
  ;; https://github.com/reddit-archive/reddit/wiki/OAuth2

  (println
    (htc/url-encode-illegal-characters
      (str
        "https://www.reddit.com/api/v1/authorize?client_id=" reddit-client-id
        "&response_type=code&state=" reddit-state
        "&redirect_uri=" (htu/url-encode redirect-uri)
        "&duration=permanent&scope="
        (str/join ","
          (map name
            [:reddit.scope/read
             :reddit.scope/submit
             :reddit.scope/identity
             :reddit.scope/modposts
             :reddit.scope/privatemessages]))))))

(declare oauth-code)

(comment
  (def oauth-code ""))

(declare access_token refresh_token)

(comment
  (def resp
    (htc/post "https://www.reddit.com/api/v1/access_token"
      {:basic-auth [reddit-client-id reddit-client-secret]
       :form-params {:grant_type "authorization_code"
                     :code oauth-code
                     :redirect_uri redirect-uri}
       :headers {"User-Agent" user-agent}
       :accept :json
       :as :json}))

  (let [{access-token :access_token
         refresh-token :refresh_token}
        (:body resp)]
    (def access_token access-token)
    (def refresh_token refresh-token))

  *e)


(defn summarize-post
  [p]
  (select-keys p
    [:id
     :name
     :url
     :title
     :permalink
     :created_utc]))

(comment
  (-> (reddit-request
        rdc
        reddit-creds
        {:method :get :reddit.api/path "/r/Clojure/hot"
         :query-params {:raw_json 1
                        :before "ezcr5d"
                        :limit 2}})
    :body :data
    :children
    (->>
      (map :data)
      (mapv summarize-post)))

  *e)

(comment

  (htc/get
    ;; https://www.reddit.com/dev/api#GET_hot
    (str "https://oauth.reddit.com/r/Clojure/hot")
    {:headers {"User-Agent" user-agent
               "Authorization" (str "bearer " access_token)}
     :query-params {:raw_json (str 1)}
     :accept :json
     :as :json}))

(defn summarize-comment
  [c]
  (-> c
    (select-keys [:id :name :author :body :created_utc :parent_id])
    (supd/supdate {:created_utc #(-> % long (* 1000) java.util.Date.)})))




(comment

  (-> (reddit-request
        rdc
        reddit-creds
        {:method :get :reddit.api/path "/r/discussion_patiente/new"
         :query-params {:raw_json 1
                        :limit 2}})
    :body :data
    :children
    (->>
      (map :data)
      (mapv summarize-post)))

  (def post-test-id "f93pdt" #_ "f791fm" #_"f7mhet")


  ;; https://www.reddit.com/dev/api#GET_comments_{article}
  (def cmts
    (-> (reddit-request
          rdc
          reddit-creds
          {:method :get
           :reddit.api/path (str "/comments/" post-test-id)
           :as :json-strict
           :query-params {:raw_json 1
                          :sort "new"
                          :threaded 0}})
      :body))

  (->> cmts
    (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "t1")) :data])
    (mapv summarize-comment)
    (mapv :created_utc)
    count)

  (def more-map
    (nth
      (->> cmts
        (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "more"))]))
      1))

  (def children-ids
    (-> more-map :data :children))

  (->> children-ids sort last)

  (def cmts2
    (-> (reddit-request
          rdc
          reddit-creds
          {:method :get
           :reddit.api/path (str "/comments/" post-test-id)
           :as :json-strict
           :query-params {:raw_json 1
                          :sort "new"
                          :threaded 0
                          :after (->> cmts
                                   (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "t1")) :data :id])
                                   sort
                                   last)}})
      :body))
  (def fetched-ids
    (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "t1")) :data :id] cmts2))

  (cset/intersection (set children-ids) (set fetched-ids))

  (count children-ids)

  ;; https://www.reddit.com/dev/api/#GET_api_morechildren
  (reddit-request
    rdc
    reddit-creds
    {:method :post
     :reddit.api/path "/api/morechildren"
     :query-params {};:raw_json 1
                    ;:api_type "json"}
     :headers {"Content-Type" "application/json"}
     :body
     (json/generate-string
       {:sort "new"
        ;:id "fipsa2p"
        ;:depth 5
        :children (-> children-ids (->> (str/join ",")))
        :link_id (str "t3_" post-test-id)})})

  (def resp *1)
  (def fetched-ids
    (sp/select
      [:body :json :data :things sp/ALL #(-> % :kind (= "t1"))
       :data :id]
      resp))

  (count fetched-ids)

  (count
    (cset/difference (set children-ids) (set fetched-ids)))

  (count
    (cset/difference (set fetched-ids) (set children-ids)))

  (reddit-request
    rdc
    reddit-creds
    {:method :get
     :reddit.api/path "/api/morechildren"
     :query-params {:raw_json 1
                    :api_type "json"
                    :sort "new"
                    :limit_children 1
                    :children (-> (cset/difference (set fetched-ids) (set children-ids)) (->> (str/join ",")))
                    :link_id (str "t3_" post-test-id)}})


  (sp/transform
    [sp/ALL :data]
    #(dissoc % :children)
    cmts)

  *e)




(comment
  ;; https://github.com/reddit-archive/reddit/wiki/OAuth2-Quick-Start-Example
  (def resp
    (htc/request
      {:method :post
       :url "https://www.reddit.com/api/v1/access_token"
       :basic-auth [reddit-client-id reddit-client-secret]
       :form-params {:grant_type "password"
                     :username reddit-username
                     :password reddit-pwd}
       :headers {"User-Agent" user-agent}
       :accept :json
       :as :json}))

  (let [{access-token :access_token}
        (:body resp)]
    (def access_token access-token))

  *e)


(comment

  (def vvv-fullname "t2_q7fwt")


  ;; https://www.reddit.com/dev/api/#POST_api_mod_conversations
  (def mod-msg
    "## Some title

Hey **you**, I saw that you commented [here](https://www.reddit.com/r/discussion_patiente/comments/f9bcav/post_test_1_2_1_2/fithns8?utm_source=share&utm_medium=web2x).")

  (reddit/reddit-request
    reddit-creds
    {:method :post
     :reddit.api/path "/api/mod/conversations"
     ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
     :form-params {:isAuthorHidden false
                   :srName "discussion_patiente"
                   :subject "Do you copy ?"
                   :body mod-msg
                   :to "vvvvalvalval"}})


  ;; https://www.reddit.com/dev/api/#POST_api_remove
  (reddit/reddit-request
    reddit-creds
    {:method :post
     :reddit.api/path "/api/remove"
     ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
     :form-params
     {:id "t1_fitl8es"
      :spam false}})


  *e)





(defmulti pat-wording (fn [wording-key lang & _args] [wording-key lang]))


(defmethod pat-wording [:pat-first-notification--subject :fr]
  [_wk _lang reddit-sub parent-cmt]
  (format "r/%s : tu pourras répondre à %s dans 24h"
    reddit-sub
    (:author parent-cmt "(utilisateur supprimé)")))


(defmethod pat-wording [:pat-first-notification--body :fr]
  [_wk _lang reddit-sub cmt parent-cmt]
  (format
    "Bonjour, ceci est un message automatique de modération de r/%s.
 Comme prévu dans le fonctionnement du forum, tu as manifesté ton intention de répondre à [%s](%s) de %s, en publiant un 'pré-commentaire',
 et ce pré-commentaire a été effacé; tu pourras publier ta véritable réponse dans **environ 24h**
 (tu recevras un message de rappel).

 Tu peux profiter de ces 24h pour réfléchir posément à ce que tu vas écrire (la nuit porte conseil)."
    reddit-sub
    (if (str/starts-with? (:reddit_parent_id cmt) "t3_")
      "cette publication"
      "ce commentaire")
    (str "https://reddit.com" (:permalink parent-cmt))
    (:author parent-cmt "(utilisateur supprimé)")))



(defn pre-comment-commands
  [reddit-sub cmt]
  (let [{user-fullname :reddit_user_fullname parent-id :reddit_parent_id} cmt
        remove-reddit-req
        {:method :post
         :reddit.api/path "/api/remove"
         :form-params
         {:id (str "t1_" (:reddit_comment_id cmt))
          :spam false}}
        write-comment-request-row
        {:reddit_parent_id parent-id
         :reddit_user_fullname user-fullname
         :pat_request_epoch_s (u/date-to-epoch-s (:reddit.comment/created_utc cmt))
         :reddit_user_name (:reddit_user_name cmt)
         :pat_sent_reminder false
         :pat_subreddit_id reddit-sub}
        send-notif-reddit-req
        (let [parent-cmt (ps/find-post-or-comment-by-fullname
                           (:reddit_parent_id cmt)
                           [:user_removed
                            :author
                            :permalink])]
          {:method :post
           :reddit.api/path "/api/mod/conversations"
           ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
           :form-params {:isAuthorHidden false
                         :srName reddit-sub
                         :subject (pat-wording :pat-first-notification--subject :fr
                                    reddit-sub parent-cmt)
                         :body (pat-wording :pat-first-notification--body :fr
                                 reddit-sub cmt parent-cmt)
                         :to (:reddit_user_name cmt)}})]
    [remove-reddit-req
     write-comment-request-row
     send-notif-reddit-req]))

(comment
  (pre-comment-commands "Clojure" cmt)

  *e)


(defmethod pat-wording [:pat-too-early-notification--subject :fr]
  [_wk _lang reddit-sub parent-cmt]
  (format "r/%s : tu as répondu trop tôt à %s"
    reddit-sub
    (:author parent-cmt "(utilisateur supprimé)")))


(defmethod pat-wording [:pat-too-early-notification--body :fr]
  [_wk _lang reddit-sub cmt parent-cmt]
  (format
    "Bonjour, ceci est un message automatique de modération de r/%s.

 Tu as répondu à [%s](%s) de %s sans respecter le délai minimum de 24h;
 par conséquent, **ta réponse a été supprimée**.

 Tu recevras un message de rappel une fois le délai écoulé: tu pourras publier
 ta réponse à partir de ce moment là."
    reddit-sub
    (if (str/starts-with? (:reddit_parent_id cmt) "t3_")
      "cette publication"
      "ce commentaire")
    (str "https://reddit.com" (:permalink parent-cmt))
    (:author parent-cmt "(utilisateur supprimé)")))


(defn too-early-commands
  [reddit-sub cmt]
  (let [remove-reddit-req
        {:method :post
         :reddit.api/path "/api/remove"
         :form-params
         {:id (str "t1_" (:reddit_comment_id cmt))
          :spam false}}
        send-notif-reddit-req
        (let [parent-cmt (ps/find-post-or-comment-by-fullname
                           (:reddit_parent_id cmt)
                           [:user_removed
                            :author
                            :permalink])]
          {:method :post
           :reddit.api/path "/api/mod/conversations"
           ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
           :form-params {:isAuthorHidden false
                         :srName reddit-sub
                         :subject (pat-wording :pat-too-early-notification--subject :fr
                                    reddit-sub parent-cmt)
                         :body (pat-wording :pat-too-early-notification--body :fr
                                 reddit-sub cmt parent-cmt)
                         :to (:reddit_user_name cmt)}})]
    [remove-reddit-req
     send-notif-reddit-req]))

(defn process-new-comment!
  [pg-db reddit-creds reddit-sub cmt]
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
          (let [[remove-reddit-req
                 write-comment-request-row
                 send-notif-reddit-req]
                (pre-comment-commands reddit-sub cmt)]
            (reddit/reddit-request reddit-creds
              remove-reddit-req)
            (jdbc/insert! pg-db "pat_comment_requests"
              write-comment-request-row)
            (reddit/reddit-request reddit-creds
              send-notif-reddit-req))
          (let [too-early? (>
                             (* 24 60 60)
                             (-
                               (u/date-to-epoch-s (:reddit.comment/created_utc cmt))
                               pat_request_epoch_s))]
            (when too-early?
              (let [[remove-reddit-req
                     send-notif-reddit-req]
                    (too-early-commands reddit-sub cmt)]
                (reddit/reddit-request reddit-creds
                  remove-reddit-req)
                (reddit/reddit-request reddit-creds
                  send-notif-reddit-req)))))))))


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
    (->> cmts
      (filter #(contains? unprocessed-ids (:reddit_comment_id %)))
      (run!
        (fn [cmt]
          (process-new-comment! pg-db reddit-creds reddit-sub
            cmt)
          (jdbc/insert! pg-db "processed_comments"
            (merge
              (select-keys cmt [:reddit_comment_id])
              {:t_processed_epoch_s (u/date-to-epoch-s (java.util.Date.))})))))))


(comment
  (def reddit-sub {})

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


(defn process-new-recent-comments-in-sub!
  [pg-db reddit-creds reddit-sub]
  (let [backward-margin-s (* 60 60)
        start-time
        (->
          (or
            (->
              (jdbc/query pg-db
                ["SELECT pat_checked_until_epoch_s FROM pat_subreddit_checkpoints
           WHERE pat_subreddit_id = ?"
                 reddit-sub]
                {:as-arrays? true})
              rest
              ffirst)
            (-> (java.util.Date.) .getTime (quot 1000)))
          (- backward-margin-s))
        end-time (-> (java.util.Date.) .getTime (quot 1000))
        recent-comments (ps/fetch-comments-in-range reddit-sub
                          [:user_removed
                           :author :author_fullname
                           :parent_id
                           :id
                           :created_utc]
                          (* start-time 1000)
                          (* end-time 1000))]
    (process-new-comments! pg-db reddit-creds reddit-sub
      recent-comments)
    (set-subreddit-checkpoint! pg-db reddit-sub end-time)))

(defn process-new-recent-comments!
  [pg-db reddit-creds reddit-subs]
  (run!
    #(process-new-recent-comments-in-sub! pg-db reddit-creds %)
    reddit-subs))

(comment
  (process-new-recent-comments! pg-db reddit-creds ["discussion_patiente"])


  (jdbc/query pg-db ["SELECT * FROM processed_comments"])
  (jdbc/query pg-db ["SELECT * FROM pat_subreddit_checkpoints"])
  (jdbc/query pg-db ["SELECT * FROM pat_comment_requests"])

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
  (ps/find-post-or-comment-by-fullname "t3_f9t460" nil)

  (set-subreddit-checkpoint! pg-db "discussion_patiente"
    (-> #inst "2020-01-01" .getTime (quot 1000)))


  (ps/fetch-comments-in-range "discussion_patiente"
    [:retrieved_on :created_utc]
    (-> #inst "2020-01-01" .getTime)
    (-> (java.util.Date.) .getTime))
  *e)







(defmethod pat-wording [:pat-24h-reminder--subject :fr]
  [_wk _lang reddit-sub to-notify parent-cmt]
  (format "r/%s : tu peux désormais répondre à %s"
    reddit-sub
    (:author parent-cmt "(utilisateur supprimé)")))


(defmethod pat-wording [:pat-24h-reminder--body :fr]
  [_wk _lang reddit-sub to-notify parent-cmt]
  (format
    "Bonjour, ceci est un message automatique de modération de r/%s.

 Le délai de 24h étant écoulé, tu peux désormais répondre [%s](%s) de %s.

 Prends tout le temps qu'il te faudra; si tu décides finalement de ne pas répondre,
 ce n'est pas un problème."
    reddit-sub
    (if (str/starts-with? (:reddit_parent_id to-notify) "t3_")
      "à la publication"
      "au commentaire")
    (str "https://reddit.com" (:permalink parent-cmt))
    (:author parent-cmt "(utilisateur supprimé)")))



(defn reminder-commands
  [pg-db reddit-subs now-epoch-s]
  (let [delay-s (* 60 60 24)
        epoch-24h-ago-s (-> now-epoch-s
                          (- delay-s))
        to-notify
        (jdbc/query pg-db
          ["SELECT reddit_parent_id, reddit_user_fullname, reddit_user_name, pat_subreddit_id
            FROM pat_comment_requests
            WHERE NOT pat_sent_reminder
            AND pat_request_epoch_s < ?"
           epoch-24h-ago-s])]
    (->> to-notify
      (mapv
        (fn [to-notify]
          (let [reddit-sub (:pat_subreddit_id to-notify)
                parent-cmt (ps/find-post-or-comment-by-fullname
                             (:reddit_parent_id to-notify)
                             [:user_removed
                              :author
                              :permalink])
                reddit-notif-req
                {:method :post
                 :reddit.api/path "/api/mod/conversations"
                 :form-params {:isAuthorHidden false
                               :srName reddit-sub
                               :subject (pat-wording :pat-24h-reminder--subject :fr
                                          reddit-sub to-notify parent-cmt)
                               :body (pat-wording :pat-24h-reminder--body :fr
                                       reddit-sub to-notify parent-cmt)
                               :to (:reddit_user_name to-notify)}}
                db-update
                ["UPDATE pat_comment_requests
                     SET pat_sent_reminder = TRUE
                     WHERE reddit_parent_id = ? AND reddit_user_fullname = ?"
                 (:reddit_parent_id to-notify)
                 (:reddit_user_fullname to-notify)]]
            [reddit-notif-req
             db-update]))))))


(defn send-reminders!
  [pg-db reddit-creds reddit-subs now-epoch-s]
  (let [reminder-commands (reminder-commands pg-db reddit-subs now-epoch-s)]
    (run!
      (fn run-reminder-commands! [[reddit-notif-req db-update]]
        (reddit/reddit-request reddit-creds reddit-notif-req)
        (jdbc/execute! pg-db db-update))
      reminder-commands)))


(comment
  (jdbc/execute! pg-db
    ["UPDATE pat_comment_requests SET pat_sent_reminder = FALSE WHERE TRUE"])

  (reminder-commands
    pg-db
    ["discussion_patiente"]
    (u/date-to-epoch-s (u/now-date)))

  (send-reminders!
    pg-db
    reddit-creds
    ["discussion_patiente"]
    (u/date-to-epoch-s (u/now-date)))

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

;; ------------------------------------------------------------------------------
;; Cross-posting


(comment
  (def reddit-sub "france")

  (def top-posts
    (->
      (reddit/reddit-request reddit-creds
        {:method :get
         :reddit.api/path (str "/r/" reddit-sub "/hot")
         :query-params {:api_type "json"
                        ;:t "hour"
                        :limit 50}})
      :body :data :children))

  (->> top-posts (map :kind) frequencies)

  (def tp (:data (nth top-posts 0)))

  ;; Cross-posting
  ;; https://www.reddit.com/r/redditdev/comments/7ejy6e/how_can_i_submit_a_crosspost_via_reddits_api_the/dq6ehey?utm_source=share&utm_medium=web2x
  (reddit/reddit-request reddit-creds
    {:method :post
     :reddit.api/path "/api/submit"
     :form-params
     {:api_type "json"
      :sr "discussion_patiente"
      :kind "crosspost"
      :title (:title tp)
      :crosspost_fullname (:name tp)}})

  *e)



(def sql_unseen-posts
  "SELECT inputs.id FROM (
    SELECT (item->>0) AS id, (item->>1) AS ad_idempotency_key
    FROM jsonb_array_elements(?::jsonb) AS item
  ) AS inputs
  WHERE NOT EXISTS (
    SELECT 1 FROM already_done WHERE
    already_done.ad_idempotency_key = inputs.ad_idempotency_key
  )")

(defn xpost-idempotency-key
  [from-sub to-sub post-fullname]
  (str "xpost|" from-sub "|" to-sub "|" post-fullname))

(defn xpost-hot-posts-from-sub!
  [pg-db reddit-creds from-sub to-sub reddit-hot-api-params]
  (let [hot-posts (->
                    (reddit/reddit-request reddit-creds
                      {:method :get
                       :reddit.api/path (str "/r/" from-sub "/hot")
                       :query-params (merge {:api_type "json"}
                                       reddit-hot-api-params)})
                    :body :data :children
                    (->>
                      (filter #(-> % :kind (= "t3")))
                      (mapv :data)))
        not-seen-fullnames
        (into #{}
          (map :id)
          (jdbc/query pg-db
            [sql_unseen-posts
             (json/generate-string
               (->> hot-posts
                 (mapv
                   (fn [post]
                     (let [idk (xpost-idempotency-key from-sub to-sub (:name post))]
                       [(:name post) idk])))))]))

        xpost-commands
        (->> hot-posts
          (filter #(-> % :name not-seen-fullnames))
          (mapv
            (let [now-epoch-s (u/date-to-epoch-s (u/now-date))]
              (fn [post]
                (let [xpost-reddit-req
                      {:method :post
                       :reddit.api/path "/api/submit"
                       :form-params
                       {:api_type "json"
                        :sr "discussion_patiente"
                        :kind "crosspost"
                        :title (:title post)
                        :crosspost_fullname (:name post)}}
                      sql-row-mark-seen
                      {:ad_idempotency_key (xpost-idempotency-key from-sub to-sub (:name post))
                       :at_t_done_epoch_s now-epoch-s}]
                  [xpost-reddit-req
                   sql-row-mark-seen])))))]
    (->> xpost-commands
      (run!
        (fn xpost! [[xpost-reddit-req
                     sql-row-mark-seen]]
          (reddit/reddit-request reddit-creds xpost-reddit-req)
          (jdbc/insert! pg-db "already_done" sql-row-mark-seen))))))

(comment
  (xpost-hot-posts-from-sub!
    pg-db reddit-creds
    "france" "discussion_patiente" {:limit 2 :count 2})

  *e)

(defn xpost-hot-posts!
  [pg-db reddit-creds from-sub+to-sub+api-param-s]
  (->> from-sub+to-sub+api-param-s
    (run!
      (fn [[reddit-hot-api-params from-sub to-sub]]
        (xpost-hot-posts-from-sub! pg-db reddit-creds
          reddit-hot-api-params from-sub to-sub)))))
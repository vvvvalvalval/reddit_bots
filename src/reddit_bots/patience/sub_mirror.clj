(ns reddit-bots.patience.sub-mirror
  "Mirroring the contents of other subreddits."
  (:require [reddit-bots.patience.reddit :as reddit]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [reddit-bots.patience.utils :as u]
            [taoensso.timbre :as log]
            [reddit-bots.patience.env.reddit :as redd]
            [reddit-bots.patience.env :as patenv]
            [reddit-bots.patience.env.effect :as pat-eff]
            [reddit-bots.patience.env.sql :as sql]))


;; ------------------------------------------------------------------------------
;; Cross-posting


(comment
  (def pat_subreddit_id "france")

  (def top-posts
    (->
      (reddit/reddit-request reddit-creds
        {:method :get
         :reddit.api/path (str "/r/" pat_subreddit_id "/hot")
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

(defn xpost-hot-posts-from-sub-commands
  [{sql-client ::patenv/sql-client
    reddit-client ::patenv/reddit-client
    now-date ::patenv/now-date
    :as pat-env}
   from-sub to-sub reddit-hot-api-params]
  (let [hot-posts (->
                    (redd/read-from-reddit reddit-client
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
          (sql/query sql-client
            [sql_unseen-posts
             (json/generate-string
               (->> hot-posts
                 (mapv
                   (fn [post]
                     (let [idk (xpost-idempotency-key from-sub to-sub (:name post))]
                       [(:name post) idk])))))]
            {}))

        xpost-commands
        (->> hot-posts
          (filter #(-> % :name not-seen-fullnames))
          (mapcat
            (let [now-epoch-s (u/date-to-epoch-s now-date)] ;; TODO from env (Val, 13 Mar 2020)
              (fn [post]
                (let [xpost-reddit-req
                      {:method :post
                       :reddit.api/path "/api/submit"
                       :form-params
                       {:api_type "json"
                        :sr to-sub
                        :kind "crosspost"
                        :title (:title post)
                        :crosspost_fullname (:name post)}}
                      sql-row-mark-seen
                      {:ad_idempotency_key (xpost-idempotency-key from-sub to-sub (:name post))
                       :at_t_done_epoch_s now-epoch-s}]
                  [{::pat-eff/effect-type ::pat-eff/change-reddit!
                    ::pat-eff/reddit-req xpost-reddit-req}
                   {::pat-eff/effect-type ::pat-eff/jdbc-insert!
                    ::pat-eff/jdbc-args ["already_done" sql-row-mark-seen]}]))))
          vec)]
    xpost-commands))

(defn xpost-hot-posts-from-sub!
  [pat-env reddit-hot-api-params from-sub to-sub]
  (let [cmds (xpost-hot-posts-from-sub-commands pat-env
               reddit-hot-api-params from-sub to-sub)]
    (run!
      (fn [cmd]
        (try
          (pat-eff/effect! pat-env cmd)
          (catch Throwable err
            (log/error err "Error cross-posting." cmd)
            (throw err))))
      cmds)))

(comment
  (xpost-hot-posts-from-sub!
    pg-db reddit-creds
    "france" "discussion_patiente" {:limit 2 :count 2})

  (xpost-hot-posts-from-sub!
    pg-db reddit-creds
    "hackernews" "patient_hackernews" {:limit 25 :count 25})

  *e)

(defn xpost-hot-posts!
  [pat-env from-sub+to-sub+api-param-s]
  (log/debug "Cross-posting hot posts...")
  (->> from-sub+to-sub+api-param-s
    (run!
      (fn [[reddit-hot-api-params from-sub to-sub]]
        (xpost-hot-posts-from-sub! pat-env
          reddit-hot-api-params from-sub to-sub)))))
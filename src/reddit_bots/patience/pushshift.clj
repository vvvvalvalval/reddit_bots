(ns reddit-bots.patience.pushshift
  (:require [clj-http.client :as htc]
            [vvvvalvalval.supdate.api :as supd]
            [reddit-bots.patience.utils :as u]))

;; See: https://github.com/pushshift/api

(defn fetch-comments-in-range
  [sub-name fields from-epoch-milli to-epoch-milli]
  (letfn [(aux [from-epoch-s]
            (let [req {:method :get
                       :url "https://api.pushshift.io/reddit/comment/search"
                       :query-params
                       {"subreddit" sub-name
                        "before" (quot to-epoch-milli 1000)
                        "after" from-epoch-s
                        "sort_type" "created_utc"
                        "sort" "asc"
                        "size" 200
                        "fields"
                        (mapv name
                          (into #{:created_utc :id}
                            fields))}
                       :accept :json
                       :as :json-strict}]
              (->
                (htc/request req)
                :body
                :data)))
          (reshape-pushshift-comment [m]
            (supd/supdate m
              [{:created_utc u/epoch-s-to-date
                :retrieved_on u/epoch-s-to-date}
               (fn [m]
                 (u/rename-keys m
                   {:user_removed :reddit.comment/user_removed
                    :author :reddit_user_name
                    :author_fullname :reddit_user_fullname
                    :parent_id :reddit_parent_id
                    :id :reddit_comment_id
                    :retrieved_on :reddit.comment/retrieved_on
                    :created_utc :reddit.comment/created_utc}))]))]
    (->> (aux (quot from-epoch-milli 1000))
      (iterate
        (fn [results]
          (if (empty? results)
            nil
            (let [max-time (->> results last :created_utc)
                  result-ids (into #{}
                               (map :id)
                               results)
                  next-batch (into []
                               (remove #(contains? result-ids (:id %)))
                               (aux (dec max-time)))]
              next-batch))))
      (take-while seq)
      (mapcat identity)
      (u/dedupe-by :id)
      (mapv reshape-pushshift-comment))))



(comment
  (fetch-comments-in-range
    "Clojure"
    (-> #inst"2020-02-25T20:38:14.000-00:00" .getTime)
    (-> #inst"2020-02-25T23:16:28.000-00:00" .getTime))
  =>
  [{:reddit_user_fullname "t2_yvkqv",
    :reddit_user_name "coffinandstone",
    :reddit_comment_id "firsdhu",
    :reddit_parent_id "t3_f9029a",
    :reddit.comment/created_utc #inst"2020-02-25T22:39:41.000-00:00"}
   {:reddit_user_fullname "t2_fbxwy",
    :reddit_user_name "xrdj6c",
    :reddit_comment_id "firsi5p",
    :reddit_parent_id "t3_f9aver",
    :reddit.comment/created_utc #inst"2020-02-25T22:40:44.000-00:00"}
   {:reddit_user_fullname "t2_5a5xj",
    :reddit_user_name "slifin",
    :reddit_comment_id "firvosn",
    :reddit_parent_id "t1_firsdhu",
    :reddit.comment/created_utc #inst"2020-02-25T23:07:56.000-00:00"}]

  (def cmt (first *1))

  *e)



(defn find-post-or-comment-by-fullname
  [fullname fields]
  (-> (htc/request
        {:method :get
         :url (format "https://api.pushshift.io/reddit/%s/search"
                (let [subname-type (subs fullname 0 3)]
                  (case subname-type
                    "t1_" "comment"
                    "t3_" "submission"
                    (throw
                      (ex-info
                        (str "Unsupported fullname type: " (pr-str subname-type))
                        {'fullname fullname})))))
         :query-params
         (merge
           {"ids" fullname}
           (when (some? fields)
             {"fields" (->> fields (mapv name))}))
         :accept :json
         :as :json-strict})
    :body
    :data
    first))

(defn find-post-by-id
  [post-id fields])

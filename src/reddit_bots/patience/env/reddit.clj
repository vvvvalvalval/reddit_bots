(ns reddit-bots.patience.env.reddit
  (:require [reddit-bots.patience.env.protocols :as env-protocols]
            [reddit-bots.patience.env.effect :as pat-eff]
            [reddit-bots.patience.reddit :as reddit]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(defn read-from-reddit
  [reddit-client req]
  (env-protocols/read-from-reddit reddit-client req))

(defn change-reddit!
  [reddit-client req]
  (env-protocols/change-reddit! reddit-client req))


(defmethod env-protocols/effect!* ::pat-eff/change-reddit! ;; INTRO issues a side-effectful call to the Reddit API. (Val, 12 Mar 2020)
  [pat-env cmd]
  (change-reddit!
    (:reddit-bots.patience.env/reddit-client pat-env)
    (::pat-eff/reddit-req cmd)))



;; ------------------------------------------------------------------------------
;; Implementations

(defrecord RealRedditClient
  [reddit-creds]
  env-protocols/RedditApiClient
  (read-from-reddit [_this req]
    (reddit/reddit-request reddit-creds req))
  (change-reddit! [_this req]
    (reddit/reddit-request reddit-creds req)))


(defn real-reddit-client
  [reddit-creds]
  (->RealRedditClient reddit-creds))



(defrecord ReadOnlyRedditClient
  [reddit-creds]
  env-protocols/RedditApiClient
  (read-from-reddit [_this req]
    (reddit/reddit-request reddit-creds req))
  (change-reddit! [_this req]
    (log/info "Pretending to" `reddit/reddit-request req)))


(defn readonly-reddit-client
  [reddit-creds]
  (->ReadOnlyRedditClient reddit-creds))



;; ------------------------------------------------------------------------------
;; Reddit utils

(defn fetch-thing-by-fullname
  [reddit-client fullname]
  (->
    (read-from-reddit reddit-client
      {:method :get
       :reddit.api/path "/api/info"
       :query-params
       {:api_type "json"
        :id (str/join "," [fullname])}})
    :body :data :children
    first
    :data))

(defn request-lazy-listing
  "Fetches a Reddit API Listing as a lazy sequence, issuing API calls as it gets realized.

  Your initial request should probably not contain any :after, :before or :count query params,
  and should probably contain a :limit query-param."
  ;; See: https://www.reddit.com/dev/api/
  [reddit-client req]
  (letfn [(extract-listing [resp]
            (-> resp :body :data))
          (next-listing [[previous-listing n]]
            (if (nil? previous-listing)
              (let [l (-> (read-from-reddit reddit-client req)
                        extract-listing)]
                [l (+ n (count (:children l)))])
              (when-some [after (:after previous-listing)]
                (let [next-req (-> req
                                 (update :query-params
                                   (fn [qp]
                                     (-> qp (or {})
                                       (merge {:after after
                                               :count n})))))
                      l (-> (read-from-reddit reddit-client next-req)
                          extract-listing)]
                  [l (+ n (count (:children l)))]))))]
    (->> [nil 0]
      (iterate next-listing)
      (take-while some?)
      (mapcat (fn [[l _n]] (:children l))))))

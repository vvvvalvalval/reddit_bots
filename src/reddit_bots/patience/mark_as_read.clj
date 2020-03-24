(ns reddit-bots.patience.mark-as-read
  "Marking the uninteresting notifications to the bot as read, so as to not clutter its inbox."
  (:require [clojure.string :as str]
            [reddit-bots.patience.env.reddit :as redd]
            [reddit-bots.patience.env.effect :as pat-eff]
            [taoensso.timbre :as log]
            [reddit-bots.patience.env :as patenv]))


;; FIXME move to other ns

(defn automod-msg?
  [{:as _msg, data :data kind :kind}]
  (and
    (= "t1" kind)
    (= "AutoModerator" (:author data))))

(defn msg-to-ignore?
  [msg]
  (automod-msg? msg))

(defn mark-msgs-to-ignore-as-read-cmds
  [pat-env]
  (into []
    (comp
      (filter msg-to-ignore?)
      (map #(-> % :data :name))
      (partition-all 100)
      (map
        (fn [fnames-to-mark]
          (let [mark-as-read-req
                {:method :post
                 :reddit.api/path "/api/read_message"
                 :form-params
                 {:id (str/join "," fnames-to-mark)}}]
            {::pat-eff/effect-type ::pat-eff/change-reddit!
             ::pat-eff/reddit-req mark-as-read-req}))))
    (redd/request-lazy-listing (::patenv/reddit-client pat-env)
      {:method :get
       :reddit.api/path "/message/unread"
       :query-params {:mark "false"}})))


(defn mark-msgs-to-ignore-as-read!
  [pat-env]
  (log/debug "Marking messages to ignore as read...")
  (run!
    (fn [cmd] (pat-eff/effect! pat-env cmd))
    (mark-msgs-to-ignore-as-read-cmds pat-env)))

(ns reddit-bots.patience.env.protocols)

(defmulti effect!* (fn [_pat-env cmd] (:reddit-bots.patience.env.effect/effect-type cmd)))

(defprotocol SqlClient
  (query [this query args])
  (insert! [this table row-map])
  (execute! [this sql-params opts]))

(defprotocol RedditApiClient
  (read-from-reddit [this req])
  (change-reddit! [this req]))


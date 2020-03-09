(ns reddit-bots.patience.main
  (:require [reddit-bots.patience.core]
            [reddit-bots.patience.utils.scheduling :as usch]
            [reddit-bots.patience.utils :as u]
            [hikari-cp.core :as hcp]))


(def reddit-subs
  ["discussion_patiente"])

(def xpost-config
  [["france" "discussion_patiente" {:limit 25 :count 25}]])

(defn start-loops
  [pg-db reddit-creds]
  (let [stop-fns
        [(usch/do-in-loop (* 1000 60 1)
           #(reddit-bots.patience.core/process-new-recent-comments!
              pg-db reddit-creds reddit-subs))
         (usch/do-in-loop (* 1000 60 1)
           #(reddit-bots.patience.core/send-reminders!
              pg-db reddit-creds reddit-subs
              (u/date-to-epoch-s (u/now-date))))
         (usch/do-in-loop (* 1000 60 60 1)
           #(reddit-bots.patience.core/xpost-hot-posts!
              pg-db reddit-creds xpost-config))]]
    (fn stop-loops! []
      (run! #(%) stop-fns))))

(defn -main [jdbc-url reddit-client-id reddit-client-secret reddit-username reddit-pwd]
  (let [reddit-creds {:reddit/username reddit-username
                      :reddit/client-id reddit-client-id
                      :reddit/client-secret reddit-client-secret
                      :reddit/password reddit-pwd}
        pg-db (hcp/make-datasource
                {:max-pool-size 10
                 :jdbc-url jdbc-url})
        stop! (start-loops pg-db reddit-creds)]
    (.addShutdownHook (Runtime/getRuntime)
      (Thread.
        ^Runnable
        (fn []
          (stop!)
          (shutdown-agents))))))

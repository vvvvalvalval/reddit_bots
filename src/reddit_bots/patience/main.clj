(ns reddit-bots.patience.main
  (:require [reddit-bots.patience.utils.scheduling :as usch]
            [reddit-bots.patience.utils :as u]
            [hikari-cp.core :as hcp]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as log-appenders]
            [reddit-bots.patience.db.postgres-setup]
            [reddit-bots.patience.new-comments]
            [reddit-bots.patience.sub-mirror]
            [reddit-bots.patience.reply-reminders]))

;; TODO protocols ? (Val, 09 Mar 2020)

(require 'sc.api) ;; FIXME (Val, 10 Mar 2020)

(do
  (log/set-level! :debug)
  (log/merge-config!
    {:appenders {:spit (log-appenders/spit-appender {:fname "data/reddit-bots.log"})}}))

(def reddit-subs
  [#_
   {:pat_subreddit_id "discussion_patiente"
    :pat_subreddit_locale :locale/fr
    :pat_subreddit_rules_url "https://www.reddit.com/r/discussion_patiente/comments/f9aka4/bienvenue_sur_rdiscussion_patiente/"}
   {:pat_subreddit_id "patient_hackernews"
    :pat_subreddit_locale :locale/en
    :pat_subreddit_rules_url "https://www.reddit.com/r/patient_hackernews/comments/fgwtlh/welcome_to_rpatient_hackernews_readme/"}])

(def xpost-config
  [#_["france" "discussion_patiente" {:limit 25 :count 25}]
   ["hackernews" "patient_hackernews" {:limit 25 :count 25}]])

(defn start-loops
  [pg-db reddit-creds]
  (let [stop-fns
        [(usch/do-in-loop (* 1000 60 1)
           (fn []
             (try
               (reddit-bots.patience.new-comments/process-new-recent-comments!
                 pg-db reddit-creds reddit-subs)
               (catch Throwable err
                 (log/error err "Error in loop iteration for" `reddit-bots.patience.new-comments/process-new-recent-comments!)))))
         (usch/do-in-loop (* 1000 60 1)
           (fn []
             (try
               (reddit-bots.patience.reply-reminders/send-reminders!
                 pg-db reddit-creds reddit-subs
                 (u/date-to-epoch-s (u/now-date)))
               (catch Throwable err
                 (log/error err "Error in loop iteration for" `reddit-bots.patience.reply-reminders/send-reminders!)))))
         (usch/do-in-loop (* 1000 60 60 1)
           (fn []
             (try
               (reddit-bots.patience.sub-mirror/xpost-hot-posts!
                 pg-db reddit-creds xpost-config)
               (catch Throwable err
                 (log/error err "Error in loop iteration for" `reddit-bots.patience.sub-mirror/xpost-hot-posts!)))))]]
    (fn stop-loops! []
      (run! #(%) stop-fns))))

(defn -main [jdbc-url reddit-client-id reddit-client-secret reddit-username reddit-pwd]
  (let [reddit-creds {:reddit/username reddit-username
                      :reddit/client-id reddit-client-id
                      :reddit/client-secret reddit-client-secret
                      :reddit/password reddit-pwd}
        pg-db {:datasource
               (hcp/make-datasource
                 {:max-pool-size 10
                  :jdbc-url jdbc-url})}
        stop! (start-loops pg-db reddit-creds)]
    (sc.api/spy) ;; FIXME
    (.addShutdownHook (Runtime/getRuntime)
      (Thread.
        ^Runnable
        (fn []
          (stop!)
          (shutdown-agents))))))


(comment ;; PROD startup
  (def pg-pwd "")


  (def jdbc-url
    (str
      "jdbc:postgresql://" "localhost:5432"
      "/" "redditbots_pg_db"
      "?user=" "redditbots_pg_user"
      "&password=" pg-pwd))

  (do
    (def reddit-client-id "Y9V30XN7RC-2EQ")
    (def reddit-username "PatientModBot"))

  (def reddit-client-secret "")
  (def reddit-pwd "")



  (-main jdbc-url reddit-client-id reddit-client-secret reddit-username reddit-pwd)

  *e)
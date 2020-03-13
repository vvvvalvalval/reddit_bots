(ns reddit-bots.patience.env.effect
  (:require [reddit-bots.patience.env.protocols :as env-protocols]))


(defn effect!
  "Runs a side-effectful action described in data by `cmd` on the environment `pat-env`."
  [pat-env cmd]
  (try
    (env-protocols/effect!* pat-env cmd)
    nil
    (catch Throwable err
      (throw
        (ex-info "Error running command."
          {::command cmd}
          err)))))
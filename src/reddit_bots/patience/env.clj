(ns reddit-bots.patience.env
  (:require [reddit-bots.patience.env.protocols :as env-protocols]
            [reddit-bots.patience.utils :as u]))

(defn refresh-now
  [pat-env]
  (assoc pat-env
    ::now-date (u/now-date)))
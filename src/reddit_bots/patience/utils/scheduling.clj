(ns reddit-bots.patience.utils.scheduling
  (:require [clojure.core.async :as a]))

(defn do-in-loop
  [period-ms f]
  (let [=ticks= (a/chan (a/dropping-buffer 1))]
    (a/go
      (loop []
        (when (a/>! =ticks= ::tick)
          (a/<! (a/timeout period-ms))
          (recur))))
    (a/thread
      (loop []
        (when (a/<!! =ticks=)
          (f)
          (recur))))
    (fn stop-loop! []
      (a/close! =ticks=))))


(defn throttler-chan
  [freq-hz buffer-size]
  (let [p-ms (long (Math/ceil (/ 1e3 freq-hz)))
        =ch= (a/chan buffer-size)]
    (a/go-loop []
      (a/>! =ch= ::go)
      (a/<! (a/timeout p-ms))
      (recur))
    =ch=))
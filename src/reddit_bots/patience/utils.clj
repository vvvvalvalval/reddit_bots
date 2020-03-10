(ns reddit-bots.patience.utils)


(defn index-by [key-fn coll]
  (into {}
    (map (fn [x]
           [(key-fn x) x]))
    coll))


(defn rename-keys
  [m oldk->newk]
  (when (some? m)
    (persistent!
      (reduce-kv
        (fn [tm oldk newk]
          (if-some [e (find m oldk)]
            (-> tm
              (dissoc! oldk)
              (assoc! newk (val e)))
            tm))
        (transient m)
        oldk->newk))))


(comment
  (rename-keys
    {:a 1 :b 2 :c 3}
    {:b :B :d :D})
  => {:a 1, :c 3, :B 2})


(defn dedupe-by
  "Returns a lazy sequence removing consecutive duplicates by the supplied fn in coll.
  Returns a transducer when no collection is provided."
  {:added "1.7"}
  ([key-fn]
   (fn [rf]
     (let [pv (volatile! ::none)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [prior @pv
                k (key-fn input)]
            (vreset! pv k)
            (if (= prior k)
              result
              (rf result input))))))))
  ([key-fn coll] (sequence (dedupe-by key-fn) coll)))


(comment
  (dedupe-by
    #(mod % 10)
    [1 11 3 9 4 12 2 8])
  => (1 3 9 4 12 8)

  *e)


(defn now-date
  ^java.util.Date []
  (java.util.Date.))

(defn epoch-s-to-date [epoch-s]
  (java.util.Date. (long (* 1000 epoch-s))))

(defn date-to-epoch-s [^java.util.Date d]
  (-> d .getTime (quot 1000)))
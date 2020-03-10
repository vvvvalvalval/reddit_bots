(ns reddit-bots.patience.i18n)

(defmulti pat-wording (fn [wording-key lang & _args] [wording-key lang]))

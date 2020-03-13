(ns reddit-bots.patience.reddit
  (:require [clojure.core.async :as a]
            [clj-http.client :as htc]
            [reddit-bots.patience.utils.scheduling :as usch]
            [clojure.string :as str]))

(defonce reddit-throttler
  (usch/throttler-chan 0.7 10))

(defn user-agent-str [reddit-client-id]
  (str "server:" reddit-client-id ":0.1" " (by /u/vvvvalvalval)"))


(defn reddit-client []
  (let [atm-username->token (atom {})]
    (letfn [(fetch-token [creds]
              (let [access_token
                    (->
                      (htc/request
                        {:method :post
                         :url "https://www.reddit.com/api/v1/access_token"
                         :basic-auth [(:reddit/client-id creds) (:reddit/client-secret creds)]
                         :form-params {:grant_type "password"
                                       :username (:reddit/username creds)
                                       :password (:reddit/password creds)}
                         :headers {"User-Agent" (user-agent-str (:reddit/client-id creds))}
                         :accept :json
                         :as :json})
                      (get-in [:body :access_token]))]
                (swap! atm-username->token assoc (:reddit/username creds) access_token)
                access_token))
            (get-token [creds]
              (or
                (get @atm-username->token (:reddit/username creds))
                (fetch-token creds)))]
      (fn request-reddit [creds req]
        (letfn [(run-req []
                  (let [user-agent (user-agent-str (:reddit/client-id creds))
                        access_token (get-token creds)
                        http-req
                        (-> req
                          (assoc :url (str "https://oauth.reddit.com" (:reddit.api/path req)))
                          (dissoc :reddit.api/path)
                          (update :headers merge
                            {"User-Agent" user-agent
                             "Authorization" (str "bearer " access_token)})
                          (->> (merge {:accept :json
                                       :as :json-strict})))]
                    (a/<!! reddit-throttler)
                    (htc/request http-req)))]
          (try
            (run-req)
            (catch Throwable err
              (if (-> err ex-data :status (= 401))
                (do
                  (fetch-token creds)
                  (run-req))
                (throw err)))))))))

(def ^:private rdc (reddit-client))

(defn reddit-request
  [reddit-creds req]
  (rdc reddit-creds req))






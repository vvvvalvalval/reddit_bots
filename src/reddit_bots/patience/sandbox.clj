(ns reddit-bots.patience.sandbox
  (:require [taoensso.timbre :as log]
            [clj-http.client :as htc]
            [clj-http.util :as htu]
            [clojure.string :as str]
            [cheshire.core :as json]
            [com.rpl.specter :as sp]
            [vvvvalvalval.supdate.api :as supd]
            [clojure.set :as cset]
            [reddit-bots.patience.utils :as u]
            [clojure.java.jdbc :as jdbc]
            [reddit-bots.patience.pushshift :as ps]
            [reddit-bots.patience.reddit :as reddit]
            [reddit-bots.patience.i18n :as i18n]))


;; App credentials: https://www.reddit.com/prefs/apps

;; Response format: https://github.com/reddit-archive/reddit/wiki/JSON

(comment
  (require 'sc.api)

  *e)

(comment
  ;; go to
  reddit-bots.patience.db.sql
  (def pg-db *1)

  *e)

(defn gen-token [l]
  (apply str
    (repeatedly l
      #(rand-nth "azertyuiopqsdfghjklmwxcvbnAZERTYUIOPQSDFGHJKLMWXCVBN1234567890"))))


(declare reddit-state)

(comment

  (def reddit-state
    (gen-token 16)))



(declare reddit-username reddit-pwd)

(do
  (def reddit-client-id "Y9V30XN7RC-2EQ")
  (def reddit-username "PatientModBot"))

(def user-agent
  (str "server:" reddit-client-id ":0.1" " (by /u/vvvvalvalval)"))

(declare reddit-creds reddit-client-secret)

(comment
  (def reddit-client-secret "")
  (def reddit-pwd "")

  (def reddit-creds
    {:reddit/username reddit-username
     :reddit/client-id reddit-client-id
     :reddit/client-secret reddit-client-secret
     :reddit/password reddit-pwd}))

(def redirect-uri "http://localhost:8080/redirect")

(comment
  ;; https://github.com/reddit-archive/reddit/wiki/OAuth2

  (println
    (htc/url-encode-illegal-characters
      (str
        "https://www.reddit.com/api/v1/authorize?client_id=" reddit-client-id
        "&response_type=code&state=" reddit-state
        "&redirect_uri=" (htu/url-encode redirect-uri)
        "&duration=permanent&scope="
        (str/join ","
          (map name
            [:reddit.scope/read
             :reddit.scope/submit
             :reddit.scope/identity
             :reddit.scope/modposts
             :reddit.scope/privatemessages]))))))

(declare oauth-code)

(comment
  (def oauth-code ""))

(declare access_token refresh_token)

(comment
  (def resp
    (htc/post "https://www.reddit.com/api/v1/access_token"
      {:basic-auth [reddit-client-id reddit-client-secret]
       :form-params {:grant_type "authorization_code"
                     :code oauth-code
                     :redirect_uri redirect-uri}
       :headers {"User-Agent" user-agent}
       :accept :json
       :as :json}))

  (let [{access-token :access_token
         refresh-token :refresh_token}
        (:body resp)]
    (def access_token access-token)
    (def refresh_token refresh-token))

  *e)


(defn summarize-post
  [p]
  (select-keys p
    [:id
     :name
     :url
     :title
     :permalink
     :created_utc]))

(comment
  (-> (reddit-request
        rdc
        reddit-creds
        {:method :get :reddit.api/path "/r/Clojure/hot"
         :query-params {:raw_json 1
                        :before "ezcr5d"
                        :limit 2}})
    :body :data
    :children
    (->>
      (map :data)
      (mapv summarize-post)))

  *e)

(comment

  (htc/get
    ;; https://www.reddit.com/dev/api#GET_hot
    (str "https://oauth.reddit.com/r/Clojure/hot")
    {:headers {"User-Agent" user-agent
               "Authorization" (str "bearer " access_token)}
     :query-params {:raw_json (str 1)}
     :accept :json
     :as :json}))

(defn summarize-comment
  [c]
  (-> c
    (select-keys [:id :name :author :body :created_utc :parent_id])
    (supd/supdate {:created_utc #(-> % long (* 1000) java.util.Date.)})))




(comment

  (-> (reddit-request
        rdc
        reddit-creds
        {:method :get :reddit.api/path "/r/discussion_patiente/new"
         :query-params {:raw_json 1
                        :limit 2}})
    :body :data
    :children
    (->>
      (map :data)
      (mapv summarize-post)))

  (def post-test-id "f93pdt" #_ "f791fm" #_"f7mhet")


  ;; https://www.reddit.com/dev/api#GET_comments_{article}
  (def cmts
    (-> (reddit-request
          rdc
          reddit-creds
          {:method :get
           :reddit.api/path (str "/comments/" post-test-id)
           :as :json-strict
           :query-params {:raw_json 1
                          :sort "new"
                          :threaded 0}})
      :body))

  (->> cmts
    (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "t1")) :data])
    (mapv summarize-comment)
    (mapv :created_utc)
    count)

  (def more-map
    (nth
      (->> cmts
        (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "more"))]))
      1))

  (def children-ids
    (-> more-map :data :children))

  (->> children-ids sort last)

  (def cmts2
    (-> (reddit-request
          rdc
          reddit-creds
          {:method :get
           :reddit.api/path (str "/comments/" post-test-id)
           :as :json-strict
           :query-params {:raw_json 1
                          :sort "new"
                          :threaded 0
                          :after (->> cmts
                                   (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "t1")) :data :id])
                                   sort
                                   last)}})
      :body))
  (def fetched-ids
    (sp/select [sp/ALL :data :children sp/ALL #(-> % :kind (= "t1")) :data :id] cmts2))

  (cset/intersection (set children-ids) (set fetched-ids))

  (count children-ids)

  ;; https://www.reddit.com/dev/api/#GET_api_morechildren
  (reddit-request
    rdc
    reddit-creds
    {:method :post
     :reddit.api/path "/api/morechildren"
     :query-params {};:raw_json 1
     ;:api_type "json"}
     :headers {"Content-Type" "application/json"}
     :body
     (json/generate-string
       {:sort "new"
        ;:id "fipsa2p"
        ;:depth 5
        :children (-> children-ids (->> (str/join ",")))
        :link_id (str "t3_" post-test-id)})})

  (def resp *1)
  (def fetched-ids
    (sp/select
      [:body :json :data :things sp/ALL #(-> % :kind (= "t1"))
       :data :id]
      resp))

  (count fetched-ids)

  (count
    (cset/difference (set children-ids) (set fetched-ids)))

  (count
    (cset/difference (set fetched-ids) (set children-ids)))

  (reddit-request
    rdc
    reddit-creds
    {:method :get
     :reddit.api/path "/api/morechildren"
     :query-params {:raw_json 1
                    :api_type "json"
                    :sort "new"
                    :limit_children 1
                    :children (-> (cset/difference (set fetched-ids) (set children-ids)) (->> (str/join ",")))
                    :link_id (str "t3_" post-test-id)}})


  (sp/transform
    [sp/ALL :data]
    #(dissoc % :children)
    cmts)

  *e)




(comment
  ;; https://github.com/reddit-archive/reddit/wiki/OAuth2-Quick-Start-Example
  (def resp
    (htc/request
      {:method :post
       :url "https://www.reddit.com/api/v1/access_token"
       :basic-auth [reddit-client-id reddit-client-secret]
       :form-params {:grant_type "password"
                     :username reddit-username
                     :password reddit-pwd}
       :headers {"User-Agent" user-agent}
       :accept :json
       :as :json}))

  (let [{access-token :access_token}
        (:body resp)]
    (def access_token access-token))

  *e)


(comment

  (def vvv-fullname "t2_q7fwt")


  ;; https://www.reddit.com/dev/api/#POST_api_mod_conversations
  (def mod-msg
    "## Some title

Hey **you**, I saw that you commented [here](https://www.reddit.com/r/discussion_patiente/comments/f9bcav/post_test_1_2_1_2/fithns8?utm_source=share&utm_medium=web2x).")

  (reddit/reddit-request
    reddit-creds
    {:method :post
     :reddit.api/path "/api/mod/conversations"
     ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
     :form-params {:isAuthorHidden false
                   :srName "discussion_patiente"
                   :subject "Do you copy ?"
                   :body mod-msg
                   :to "vvvvalvalval"}})


  ;; https://www.reddit.com/dev/api/#POST_api_remove
  (reddit/reddit-request
    reddit-creds
    {:method :post
     :reddit.api/path "/api/remove"
     ;; NOTE important NOT to json-encode the body (Val, 26 Feb 2020)
     :form-params
     {:id "t1_fitl8es"
      :spam false}})


  *e)







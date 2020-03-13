(ns reddit-bots.patience.env.sql
  (:require [reddit-bots.patience.env.protocols :as env-protocols]
            [reddit-bots.patience.env.effect :as pat-eff]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn query
  [sql-client query args]
  (env-protocols/query sql-client query args))

(defn insert!
  [sql-client table-name row-map]
  (env-protocols/insert! sql-client table-name row-map))

(defn execute!
  [sql-client sql-params opts]
  (env-protocols/execute! sql-client sql-params opts))


(defmethod env-protocols/effect!* ::pat-eff/jdbc-insert!
  [pat-env cmd]
  (apply insert!
    (:reddit-bots.patience.env/sql-client pat-env)
    (::pat-eff/jdbc-args cmd)))

(defmethod env-protocols/effect!* ::pat-eff/jdbc-execute!
  [pat-env cmd]
  (apply execute!
    (:reddit-bots.patience.env/sql-client pat-env)
    (::pat-eff/jdbc-args cmd)))

;; ------------------------------------------------------------------------------
;; Implementations

(defrecord ReadOnlySqlClient
  [pg-db]
  env-protocols/SqlClient
  (query [_this query args]
    (jdbc/query pg-db query args))
  (insert! [_this table-name row-map]
    (log/info "Pretending to" `jdbc/insert! table-name row-map))
  (execute! [_this sql-params opts]
    (log/info "Pretending to" `jdbc/execute! sql-params opts)))

(defn readonly-sql-client
  [pg-db]
  (->ReadOnlySqlClient pg-db))



(defrecord RealSqlClient
  [pg-db]
  env-protocols/SqlClient
  (query [_this query args]
    (jdbc/query pg-db query args))
  (insert! [_this table-name row-map]
    (jdbc/insert! pg-db table-name row-map))
  (execute! [_this sql-params opts]
    (jdbc/execute! pg-db sql-params opts)))

(defn real-sql-client
  [pg-db]
  (->RealSqlClient pg-db))
(ns reddit-bots.patience.db.sql
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [reddit-bots.patience.db.postgres-setup]
            [hikari-cp.core :as hcp]))


(comment

  ;; Finding the DB URL
  (defn pg-url
    [domain-port db-name user-name password]
    (str
      "jdbc:postgresql://" domain-port
      "/" db-name
      "?ssl=true&user=" user-name
      "&password=" password))

  (def pwd "")

  (def pg-db
    {:datasource
     (hcp/make-datasource
       {:max-pool-size 10
        :jdbc-url
        (str
          "jdbc:postgresql://" "localhost:5432"
          "/" "reddit_bots_dev_db"
          "?user=" "val"
          "&password=" pwd)})})


  ;; Creating the tables
  (jdbc/execute! pg-db
    [(slurp (io/resource "reddit_bots/patience/db/install-schema.sql"))])

  ;; FIXME clean

  ;; Creating the users the users
  ;; There are essentially 2 roles: one for machines and one for devs
  ;; You add and remove users to the roles
  ;; see https://www.postgresql.org/docs/9.1/static/sql-createrole.html
  ;; and https://www.postgresql.org/docs/9.1/static/sql-grant.html
  (jdbc/db-do-commands pg-db
    ["CREATE ROLE privacy_store_client"
     (str
       "GRANT SELECT, INSERT, UPDATE"
       " ON TABLE privacy_store_entries"
       " TO privacy_store_client")])


  (defn create-client-user
    "Creates a user Postgresql role, intended to be used by a machine."
    [pg-db user-name pass]
    (jdbc/execute! pg-db
      [(str
         "CREATE ROLE " user-name
         " WITH LOGIN PASSWORD " "'" pass "'"
         " IN ROLE privacy_store_client")]))

  (jdbc/db-do-commands pg-db
    ["CREATE ROLE bs_dev"
     (str
       "GRANT ALL PRIVILEGES ON DATABASE bs_pg_prod_db"
       " TO bs_dev")
     (str
       "GRANT ALL PRIVILEGES ON TABLE privacy_store_entries"
       " TO bs_dev")])

  (defn create-dev-user
    "Creates a user Postgresql role, intended to be used by a developer."
    [pg-db user-name pass]
    (jdbc/execute! pg-db
      [(str
         "CREATE ROLE " user-name
         " WITH LOGIN PASSWORD " "'" pass "'"
         " IN ROLE bs_dev")])))

(comment
  ;; CF to_timestamp https://www.postgresql.org/docs/10/functions-datetime.html#FUNCTIONS-DATETIME-TABLE

  (jdbc/insert-multi! pg-db "pat_subreddit_checkpoints"
    [{:pat_subreddit_id "discussion_patiente"
      :pat_checked_until_epoch_s #inst "2020-02-24"}])


  *e)


(comment

  (jdbc/execute! pg-db
    [(slurp (io/resource "reddit_bots/patience/db/install-schema.sql"))])

  (jdbc/execute! pg-db
    ["
    DROP TABLE already_done;
    DROP TABLE processed_comments;
    DROP TABLE pat_comment_requests;
    DROP TABLE pat_subreddit_checkpoints;"])

  *e)




















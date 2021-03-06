(ns net.thegeez.thecljvector.database.migrations
  (:require [io.pedestal.log :as log]
            [clojure.java.jdbc :as jdbc]
            [net.thegeez.w3a.components.sql-database.migrator :as migrator]))

(defn serial-id [db]
  (if (.contains (:connection-uri db) "derby")
    [:id "INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)"]
    [:id :serial "PRIMARY KEY"]))

(def migrations
  [[1 migrator/version-migration]
   (let [table :users]
     [2 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:username "VARCHAR(256) UNIQUE"]
                    [:password_encrypted "VARCHAR(256)"]
                    [:github_id "VARCHAR(256) UNIQUE"]
                    [:google_id "VARCHAR(256) UNIQUE"]
                    [:facebook_id "VARCHAR(256) UNIQUE"]
                    [:twitter_id "VARCHAR(256) UNIQUE"]
                    [:hidden "BOOLEAN default false"]
                    [:admin "BOOLEAN default false"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])
   (let [table :posts]
     [3 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:title "VARCHAR(1024)"]
                    [:link "VARCHAR(1024)"]
                    [:text "VARCHAR(10240)"]
                    [:user_id "BIGINT"]
                    [:hidden "BOOLEAN default false"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])
   (let [table :comments]
     [4 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:post_id "BIGINT"]
                    [:path "VARCHAR(10240) UNIQUE"]
                    [:user_id "BIGINT"]
                    [:text "VARCHAR(1024)"]
                    [:hidden "BOOLEAN DEFAULT false"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])])

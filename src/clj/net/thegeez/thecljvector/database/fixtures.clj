(ns net.thegeez.thecljvector.database.fixtures
  (:require [buddy.hashers :as hashers]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [clojure.java.jdbc :as jdbc]))

(defn insert-fixtures! [db]
  (let [now (.getTime (java.util.Date.))]
    (jdbc/insert! db :users
                  {:username "amy"
                   :password_encrypted (hashers/encrypt "amy")
                   :admin true
                   :created_at now
                   :updated_at now}
                  {:username "bob"
                   :password_encrypted (hashers/encrypt "bob")
                   :created_at now
                   :updated_at now})
    (let [[amy-id bob-id] (map #(:id (first (jdbc/query db ["select id from users where username = ?" %]))) ["amy" "bob"])]
      (jdbc/insert! db :posts
                    {:title "My first title"
                     :text "some text"
                     :user_id amy-id
                     :hidden true
                     :created_at now
                     :updated_at now}
                    {:title "How meta"
                     :link "http://localhost:8080"
                     :user_id bob-id
                     :created_at now
                     :updated_at now})
      (let [post-id (:id (first (jdbc/query db ["select * from posts"])))]
        (jdbc/insert! db :comments
                      {:post_id post-id
                       :path "1.-8J1yF0"
                       :user_id amy-id
                       :text "1.-8J1yF0 First comment by amy"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF0.-8J1yF0"
                       :user_id bob-id
                       :text "1.-8J1yF0.-8J1yF0 First child by bob"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF1"
                       :user_id bob-id
                       :text "1.-8J1yF1 Second child by bob"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF2"
                       :user_id bob-id
                       :text "1.-8J1yF2 Second child by bob"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF0.-8J1yF0.-8J1yF0"
                       :user_id amy-id
                       :text "1.-8J1yF0.-8J1yF0.-8J1yF0 Three deep reply by amy"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF0.-8J1yF0.-8J1yF0.-8J1yF0"
                       :user_id amy-id
                       :text "1.-8J1yF0.-8J1yF0.-8J1yF0.-8J1yF0 Three deep reply by amy"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF0.-8J1yF1"
                       :user_id amy-id
                       :text "1.-8J1yF0.-8J1yF1 Hello"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF1.-8J1yF0"
                       :user_id bob-id
                       :text "1.-8J1yF1.-8J1yF0 Second comment by bob"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF1.-8J1yF0.-8J1yF0"
                       :user_id bob-id
                       :text "1.-8J1yF1.-8J1yF0.-8J1yF0 Second comment by bob"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF1.-8J1yF0.-8J1yF1"
                       :user_id bob-id
                       :text "1.-8J1yF1.-8J1yF0.-8J1yF1 Second comment by bob"
                       :created_at now
                       :updated_at now}
                      {:post_id post-id
                       :path "1.-8J1yF2.-8J1yF0"
                       :user_id bob-id
                       :text "1.-8J1yF2.-8J1yF0 Second comment by bob"
                       :created_at now
                       :updated_at now}
                      )))))

(defrecord Fixtures [database]
  component/Lifecycle
  (start [component]
    (log/info :msg "Starting fixture loader")
    (when-not (:loaded-fixtures component)
      (try
        (insert-fixtures! (:spec database))
        (catch Exception e
          (log/info :loading-fixtures-failed (.getMessage e)))))
    (assoc component :loaded-fixtures true))

  (stop [component]
    (log/info :msg "Stopping fixture loader")
    component))

(defn fixtures []
  (map->Fixtures {}))

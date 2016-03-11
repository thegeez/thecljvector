(ns net.thegeez.thecljvector.users
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]
            [io.pedestal.interceptor :as interceptor]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.link :as link]))

(defn find-or-create-by-facebook-id [context facebook-id]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE facebook_id = ? " facebook-id]))]
      (select-keys user [:id :username :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:facebook_id facebook-id
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context facebook-id)))))

(defn find-or-create-by-github-id [context github-id]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE github_id = ? " github-id]))]
      (select-keys user [:id :username :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:github_id github-id
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context github-id)))))

(defn find-or-create-by-google-id [context google-id]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE google_id = ? " google-id]))]
      (select-keys user [:id :username :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:google_id google-id
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context google-id)))))

(defn find-or-create-by-twitter-id [context twitter-id]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE twitter_id = ? " twitter-id]))]
      (select-keys user [:id :username :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:twitter_id twitter-id
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context twitter-id)))))


(defn set-name [context id username]
  (try
    (let [res (jdbc/update! (:database context)
                            :users
                            {:username username
                             :updated_at (.getTime (java.util.Date.))}
                            ["id = ?" id])]
      (when-not (= 1 (count res))
        (throw (Exception.)))
      id)
    (catch Exception _
      ;; assume name unique violation
      (log/info :create-name-e _)
      {:errors {:username ["Username already exists"]}})))

(defn create-user [context credentials]
  (let [{:keys [username password]} credentials
        _ (assert (and (seq username)
                       (seq password)))
        now (.getTime (java.util.Date.))]
    (try
      (let [res (jdbc/insert! (:database context)
                              :users
                              {:username username
                               :password_encrypted (hashers/encrypt password)
                               :created_at now
                               :updated_at now})]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (if-let [derby-id (:1 (first res))]
          (long derby-id)
          (:id (first res))))
      (catch Exception _
        {:errors {:username ["Username already exists"]}}))))

(defn user-resource [context data]
  (let [{:keys [username]} data]
    (-> data
        (assoc-in [:links :self] (link/link context :users/show :params {:username username})))))

(defn get-user [db username]
  (when-let [user (first (jdbc/query db ["SELECT id, username, created_at, updated_at FROM users WHERE username = ?" username]))]
    user))

(def with-user
  (interceptor/interceptor
   {:enter (fn [context]
             (let [username (get-in context [:request :path-params :username])]
               (if-let [user (get-user (:database context) username)]
                 (assoc context :user (user-resource context user))
                 (context/terminate context 404))))}))

(def show
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:user (get-in context [:user])
                             :links {:home (link/link context :home)}}}}))}))

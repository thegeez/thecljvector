(ns dev-system
  (:require [ns-tracker.core :refer [ns-tracker]]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [io.pedestal.http :as http]
            [net.thegeez.w3a.components.server :as server]
            [net.thegeez.w3a.components.sql-database :as sql-database]
            [net.thegeez.w3a.components.sql-database.migrator :as migrator]
            [ring.middleware.session.cookie]
            [net.thegeez.thecljvector.service :as service]
            [net.thegeez.thecljvector.database.migrations :as migrations]
            [net.thegeez.thecljvector.database.fixtures :as fixtures]))

(def modified-namespaces (ns-tracker "src/clj"))

(defn dev-service [service]
    (-> service ;; start with production configuration
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::http/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes #(do
                                 (doseq [ns-sym (modified-namespaces)]
                                   (require ns-sym :reload))
                                 (deref #'service/routes))
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}})
      http/dev-interceptors))

(def dev-config {:db-connect-string "jdbc:derby:memory:thecljvector;create=true"
                 :port 8080
                 :migrations migrations/migrations})

(defn dev-system []
  (log/info :msg "Hello world, this is the development system!")
  (let [{:keys [db-connect-string port migrations]} dev-config]
    (component/system-map
     :session-options {:store (ring.middleware.session.cookie/cookie-store {:key "UNSAFE_CHANGEME!"})}
     :server (component/using
              (server/pedestal-component (dev-service
                                          (assoc service/service
                                            ::http/port port
                                            ::server/component->context {:database [:database :spec]})))
              {:database :db
               :session-options :session-options})
     :jetty (component/using
             (server/jetty-component)
             [:server])
     :db (sql-database/sql-database db-connect-string)
     :db-migrator (component/using
                   (migrator/dev-migrator migrations)
                   {:database :db})
     :fixtures (component/using
                (fixtures/fixtures)
                {:database :db
                 :db-migrator :db-migrator}))))

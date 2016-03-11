(ns net.thegeez.thecljvector.admin
  (:require [clojure.java.jdbc :as jdbc]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.log :as log]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.w3a.link :as link]))


(def ban-post
  (interceptor/interceptor
   {:enter (fn [context]
             (let [auth (get-in context [:auth])]
               (if-not (:admin auth)
                 (merge context
                        {:response
                         {:status 403
                          :headers {"Content-Type" "text/html"}
                          :body "Not allowed"}})
                 (let [post-id (get-in context [:request :path-params :id])
                       database (:database context)
                       hide-post (jdbc/update! database
                                               :posts
                                               {:hidden true}
                                               ["id = ?" post-id])]
                   (merge context
                          {:response
                           {:status 200
                            :headers {"Content-Type" "text/html"}
                            :body (pr-str {:hide-post hide-post
                                           })}})))))}))

(def unban-post
  (interceptor/interceptor
   {:enter (fn [context]
             (let [auth (get-in context [:auth])]
               (if-not (:admin auth)
                 (merge context
                        {:response
                         {:status 403
                          :headers {"Content-Type" "text/html"}
                          :body "Not allowed"}})
                 (let [post-id (get-in context [:request :path-params :id])
                       database (:database context)
                       hide-post (jdbc/update! database
                                               :posts
                                               {:hidden false}
                                               ["id = ?" post-id])]
                   (merge context
                          {:response
                           {:status 200
                            :headers {"Content-Type" "text/html"}
                            :body (pr-str {:hide-post hide-post
                                           })}})))))}))

(def ban-user
  (interceptor/interceptor
   {:enter (fn [context]
             (let [auth (get-in context [:auth])]
               (if-not (:admin auth)
                 (merge context
                        {:response
                         {:status 403
                          :headers {"Content-Type" "text/html"}
                          :body "Not allowed"}})
                 (let [user (:user context)
                       user-id (:id user)
                       database (:database context)
                       hide-user (jdbc/update! database
                                               :users
                                               {:hidden true}
                                               ["id = ?" user-id])
                       hide-posts (jdbc/update! database
                                                :posts
                                                {:hidden true}
                                                ["user_id = ?" user-id])
                       hide-comments (jdbc/update! database
                                                   :comments
                                                   {:hidden true}
                                                   ["user_id = ?" user-id])]
                   (merge context
                          {:response
                           {:status 200
                            :headers {"Content-Type" "text/html"}
                            :body (pr-str {:user user
                                           :hide-user hide-user
                                           :hide-posts hide-posts
                                           :hide-comments hide-comments
                                           })}})))))}))

(def unban-user
  (interceptor/interceptor
   {:enter (fn [context]
             (let [auth (get-in context [:auth])]
               (if-not (:admin auth)
                 (merge context
                        {:response
                         {:status 403
                          :headers {"Content-Type" "text/html"}
                          :body "Not allowed"}})
                 (let [user (:user context)
                       user-id (:id user)
                       database (:database context)
                       hide-user (jdbc/update! database
                                               :users
                                               {:hidden false}
                                               ["id = ?" user-id])
                       hide-posts (jdbc/update! database
                                                :posts
                                                {:hidden false}
                                                ["user_id = ?" user-id])
                       hide-comments (jdbc/update! database
                                                   :comments
                                                   {:hidden false}
                                                   ["user_id = ?" user-id])]
                   (merge context
                          {:response
                           {:status 200
                            :headers {"Content-Type" "text/html"}
                            :body (pr-str {:user user
                                           :hide-user hide-user
                                           :hide-posts hide-posts
                                           :hide-comments hide-comments
                                           })}})))))}))

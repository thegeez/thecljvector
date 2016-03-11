(ns net.thegeez.thecljvector.home
  (:require [io.pedestal.interceptor :as interceptor]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.link :as link]
            [net.thegeez.thecljvector.posts :as posts]))

(def home
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
               {:response
                {:status 200
                 :data {:posts (posts/get-posts context)}}}))}))

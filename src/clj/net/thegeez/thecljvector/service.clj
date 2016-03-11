(ns net.thegeez.thecljvector.service
  (:require [io.pedestal.log :as log]
            [io.pedestal.http :as http]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [net.thegeez.w3a.breadcrumb :as breadcrumb]
            [net.thegeez.w3a.edn :as edn]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.w3a.link :as link]
            [net.thegeez.w3a.oauth.facebook :as oauth.facebook]
            [net.thegeez.w3a.oauth.github :as oauth.github]
            [net.thegeez.w3a.oauth.google :as oauth.google]
            [net.thegeez.w3a.oauth.twitter :as oauth.twitter]
            [net.thegeez.thecljvector.admin :as admin]
            [net.thegeez.thecljvector.auth :as auth]
            [net.thegeez.thecljvector.auth.view :as auth.view]
            [net.thegeez.thecljvector.home :as home]
            [net.thegeez.thecljvector.comments :as comments]
            [net.thegeez.thecljvector.comments.view :as comments.view]
            [net.thegeez.thecljvector.posts :as posts]
            [net.thegeez.thecljvector.posts.view :as posts.view]
            [net.thegeez.thecljvector.users :as users]
            [net.thegeez.thecljvector.users.view :as users.view]
            ))

(def with-menu-links
  (interceptor/interceptor
   {:enter (fn [context]
             (assoc context
                    :menu-links {:add-link (link/link context :posts/create)
                                 :home (link/link context :home)}))}))

(defroutes
  routes
  [[["/"
     ^:interceptors [auth/with-auth
                     (html/for-html 404 (constantly "Not found"))
                     with-menu-links]

     {:get [:home
            ^:interceptors [(html/for-html 200 posts.view/html-render-index)]
            home/home]}
     ["/post"
      ["/new"
       ^:interceptors [auth/require-authentication
                       (html/for-html 200 posts.view/html-render-rate-limit)
                       posts/post-rate-limiter
                       (html/for-html 200 posts.view/html-render-new)
                       (html/for-html 422 posts.view/html-render-new)]
       {:get
        [:post/new
         posts/new]
        :post
        [:posts/create
         ^:interceptors [(form/parse-form :post posts/create-post-form)]
         posts/create]}]
      ["/:id"
       ^:interceptors [(link/coerce-path-params {:id :long})
                       posts/with-post]
       {:get
        [:posts/show
         ^:interceptors [(html/for-html 200 posts.view/html-render-show)]
         posts/show]}
       ["/ban"
        {:get admin/ban-post}]
       ["/unban"
        {:get admin/unban-post}]]]
     ["/comments"
      ["/new"
       {:post
        [:comments/create
         ^:interceptors [auth/require-authentication
                         (form/parse-form :comment comments/create-comment-form)]
         comments/create]}]]

     ["/login"
      {:get
       [:auth/login
        ^:interceptors [(html/for-html 200 auth.view/html-render-login)]
        auth/login]
       :post [:auth/login-post
              ^:interceptors [(html/for-html 422 auth.view/html-render-login)
                              (form/parse-form :login-credentials auth/login-form)]
              auth/login-post]}]
     ["/signup"
      {:get
       [:auth/signup
        ^:interceptors [(html/for-html 200 auth.view/html-render-login)]
        auth/login]
       :post [:auth/signup-post
              ^:interceptors [(html/for-html 422 auth.view/html-render-login)
                              (form/parse-form :signup-credentials auth/signup-form)]
              auth/signup-post]}]
     ["/logout" {:post [:auth/logout auth/logout-post]}]

     ["/oauth"
      ^:interceptors [auth/return-if-logged-in]
      ["/facebook"
       ["/authenticate" {:get [:auth/oauth-facebook oauth.facebook/authenticate]}]
       ["/callback" {:get [:auth/oauth-facebook-callback (oauth.facebook/callback users/find-or-create-by-facebook-id)]}]]
      ["/github"
       ["/authenticate" {:get [:auth/oauth-github oauth.github/authenticate]}]
       ["/callback" {:get [:auth/oauth-github-callback (oauth.github/callback users/find-or-create-by-github-id)]}]]
      ["/google"
       ["/authenticate" {:get [:auth/oauth-google oauth.google/authenticate]}]
       ["/callback" {:get [:auth/oauth-google-callback (oauth.google/callback users/find-or-create-by-google-id)]}]]
      ["/twitter"
       ["/authenticate" {:get [:auth/oauth-twitter oauth.twitter/authenticate]}]
       ["/callback" {:get [:auth/oauth-twitter-callback (oauth.twitter/callback users/find-or-create-by-twitter-id)]}]]

      ["/create-name"
       {:get [:auth/oauth-create-name
              ^:interceptors [(html/for-html 200 auth.view/html-render-create-name)]
              auth/create-name]
        :post [:auth/oauth-create-name-post
               ^:interceptors [(html/for-html 422 auth.view/html-render-create-name)
                               (form/parse-form :create-name auth/create-name-form)]
               auth/create-name-post]}]]
     ["/users/:username"
      {:get
       [:users/show
        ^:interceptors [(html/for-html 200 users.view/html-render-show)
                        users/with-user]
        users/show]}
      ["/ban"
       {:get
        [^:interceptors [users/with-user]
         admin/ban-user]}]
      ["/unban"
       {:get
        [^:interceptors [users/with-user]
         admin/unban-user]}]]
     ]]])

(def bootstrap-webjars-resource-path "META-INF/resources/webjars/bootstrap/3.3.4")
(def jquery-webjars-resource-path "META-INF/resources/webjars/jquery/1.11.1")

(def service
  {:env :prod
   ::http/router :linear-search

   ::http/routes routes

   ::http/resource-path "/public"

   ::http/default-interceptors [(middlewares/resource bootstrap-webjars-resource-path)
                                (middlewares/resource jquery-webjars-resource-path)]

   ::http/type :jetty
   })

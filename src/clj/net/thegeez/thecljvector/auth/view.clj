(ns net.thegeez.thecljvector.auth.view
  (:require [net.cgrand.enlive-html :as enlive]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.thecljvector.layout :as layout]
            [net.thegeez.thecljvector.auth :as auth]))

(layout/application html-render-login
                    [context]
                    [:.navbar] nil
                    [:#menubar] nil
                    [:#content] (enlive/before
                                 (enlive/html [:div.row
                                               [:div.col-sm-6.signup-right-border
                                                [:h1 "The CLJ Vector signup"]
                                                [:div.panel.panel-default
                                                 [:div.panel-body
                                                  (form/html-form context {:action (get-in context [:response :data :links :signup])
                                                                           :fields (form/form-fields :signup-credentials
                                                                                                     auth/signup-form
                                                                                                     (get-in context [:response :data :signup-credentials]))
                                                                           :submit "Signup"})]]
                                                                                                [:div.panel.panel-default
                                                 [:div.panel-body "Signup with "
                                                  [:a {:href (get-in context [:response :data :links :github])} "GitHub"]
                                                  " and choose a username afterwards"]]
                                                [:div.panel.panel-default
                                                 [:div.panel-body "Signup with "
                                                  [:a {:href (get-in context [:response :data :links :google])} "Google"]
                                                  " and choose a username afterwards"]]
                                                [:div.panel.panel-default
                                                 [:div.panel-body "Signup with "
                                                  [:a {:href (get-in context [:response :data :links :facebook])} "Facebook"]
                                                  " and choose a username afterwards"]]
                                                [:div.panel.panel-default
                                                 [:div.panel-body "Signup with "
                                                  [:a {:href (get-in context [:response :data :links :twitter])} "Twitter"]
                                                  " and choose a username afterwards"]]]
                                               [:div.col-sm-6
                                                [:h1 "The CLJ Vector login"]
                                                [:div.panel.panel-default
                                                 [:div.panel-body
                                                  (form/html-form context {:action (get-in context [:response :data :links :login])
                                                                           :fields (form/form-fields :login-credentials
                                                                                                     auth/login-form
                                                                                                     (get-in context [:response :data :login-credentials]))
                                                                           :submit "Login"})]]
                                                [:div.panel.panel-default
                                                 [:div.panel-body "Login with "
                                                  [:a {:href (get-in context [:response :data :links :github])} "GitHub"]]]
                                                [:div.panel.panel-default
                                                 [:div.panel-body "Login with "
                                                  [:a {:href (get-in context [:response :data :links :google])} "Google"]]]
                                                [:div.panel.panel-default
                                                 [:div.panel-body "Login with "
                                                  [:a {:href (get-in context [:response :data :links :facebook])} "Facebook"]]]
                                                [:div.panel.panel-default
                                                 [:div.panel-body "Login with "
                                                  [:a {:href (get-in context [:response :data :links :twitter])} "Twitter"]]]]])))

(layout/application html-render-create-name
                    [context]
                    [:.navbar] nil
                    [:#content] (enlive/before
                                 (enlive/html [:h1 "The CLJ Vector signup"]
                                              [:div.panel.panel-default
                                               [:div.panel-body
                                                (form/html-form context {:action (get-in context [:response :data :links :create-name])
                                                                         :fields (form/form-fields :create-name
                                                                                                   auth/create-name-form
                                                                                                   (get-in context [:response :data :create-name]))
                                                                         :submit "Create username"})]])))

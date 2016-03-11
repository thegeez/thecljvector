(ns net.thegeez.thecljvector.users.view
  (:require [net.cgrand.enlive-html :as enlive]
            [net.thegeez.w3a.breadcrumb :as breadcrumb]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.thecljvector.layout :as layout]
            [net.thegeez.thecljvector.users :as users]))

(layout/application html-render-index
  [context]
  [:#content] (enlive/html-content
               (html/edn->html (get-in context [:response :data]))))

(layout/application html-render-show
                    [context]
                    [:#content] (enlive/before
                                 (enlive/html
                                  [:div
                                   [:h3 "Show user"]
                                   [:form.form-horizontal
                                    (for [[k v] (get-in context [:response :data :user])]
                                      [:div.form-group
                                       [:label.control-label.col-sm-2
                                        (get
                                         {:username "Username"
                                          :self "Self"}
                                         k k)]
                                       [:div.col-sm-10
                                        [:p.form-control-static
                                         (cond
                                          (= k :links)
                                          (interpose ", "
                                                     (for [[title href] v]
                                                       [:span title " "[:a {:href href} href]]))
                                          (contains? #{:created_at :updated_at} k)
                                          (str v " / " (java.util.Date. v))
                                          :else
                                          v) ]]])]]))
                    [:#content] (enlive/html-content
                                 (html/edn->html (get-in context [:response :data]))))

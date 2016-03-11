(ns net.thegeez.thecljvector.posts.view
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as enlive]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.thecljvector.layout :as layout]
            [net.thegeez.thecljvector.time :as time]
            [net.thegeez.thecljvector.posts :as posts]
            [net.thegeez.thecljvector.comments :as comments]
            [net.thegeez.thecljvector.comments.view :as comments.view]))

(def index-table-template (enlive/html-resource "templates/index_table.html"))

(defn show-posts [posts]
  (enlive/at index-table-template
             [:ul :li] (enlive/clone-for
                        [{:keys [id type username title link text host post-link comment-count user_id created_at] :as post} posts]
                        [:a.link] (enlive/do->
                                   (enlive/set-attr :href post-link)
                                   (enlive/content title))
                        [:span.host] (when host
                                       (enlive/transformation
                                        [:a.host] (enlive/do->
                                                   (enlive/set-attr :href link)
                                                   (enlive/content link))))
                        [:a.username] (enlive/do->
                                       (enlive/set-attr :href (:user (:links post)))
                                       (enlive/content username))
                        [:span.timestamp] (enlive/content
                                           (enlive/html (time/time-ago-tags created_at)))
                        [:a.post-link] (enlive/do->
                                        (enlive/set-attr :href post-link)
                                        (enlive/content (cond
                                                          (zero? comment-count)
                                                          "no comments"
                                                          (= comment-count 1)
                                                          "1 comment"
                                                          :else
                                                          (str comment-count " comments")))))))

(layout/application html-render-index
                    [context]
                    [:#content] (enlive/before
                                 (show-posts (get-in context [:response :data :posts])))

                    #_#_[:#content] (enlive/html-content
                                 (html/edn->html (get-in context [:response :data]))))



(layout/application html-render-show
                    [context]
                    [:#content] (let [post (get-in context [:response :data :post])]
                                  (if (:hidden post)
                                    (enlive/append
                                     (enlive/html
                                      [:div "This post is awaiting moderation"]))
                                    (enlive/do->
                                     (enlive/append
                                      (show-posts [post]))
                                     (enlive/append
                                      (enlive/html
                                       (when-let [text (:text post)]
                                         [:div.text [:p text]])))
                                     (enlive/append
                                      (enlive/html
                                       (comments.view/comments-thread context)))
                                     (enlive/append
                                      (enlive/html
                                       [:script {:type "text/javascript"}
                                        "function highlightComment() {
var eid = \"#comment-\" + window.location.hash.substring(1);
$(\".highlight\").removeClass(\"highlight\");
$(eid).addClass(\"highlight\");
};
                                    window.onload = highlightComment;"])))))
                    #_#_                    [:#content] (let [post (get-in context [:response :data :post])]
                                                          (if-let [text (:text post)]
                                                            (enlive/append
                                                             (enlive/html
                                                              [:div.text text]))
                                                            identity))
                    #_#_[:#content] (enlive/html-content (html/edn->html (flat->nest (get-in context [:response :data :post :comments]))))


                    #_#_[:#content] (enlive/html-content
                                     (html/edn->html (get-in context [:response :data]))))

(defmethod form/render-form-field :post/textarea
  [{:keys [id label name] :as def} value errors]
  [:div
   {:class (str "form-group"
                (when errors
                  " has-error"))}
   [:label.control-label
    {:for name} label]
   [:textarea.form-control
    {:id name
     :name name} value]
   (when errors
     (for [error errors]
       [:span.help-block
        error]))])

(layout/application html-render-rate-limit
  [context]
  [:#content] (enlive/before
               (enlive/html [:h1 "Add a post to The CLJ Vector"]
                            [:p "You may only add one link every 24 hours. Your last post was at: " (get-in context [:response :data :recent-post-time])])))

(layout/application html-render-new
  [context]
  [:#content] (enlive/before
               (enlive/html [:h1 "Add a post to The CLJ Vector"]
                            [:p "You may add a link or a text post, but not both"]
                            (form/html-form context {:action (get-in context [:response :data :links :create])
                                                     :fields (form/form-fields :post
                                                                               posts/create-post-form
                                                                               (get-in context [:response :data :post]))
                                                     :submit "Submit post"})))
  #_#_[:#content] (enlive/html-content
               (html/edn->html (get-in context [:response :data]))))

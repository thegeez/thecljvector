(ns net.thegeez.thecljvector.comments.view
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as enlive]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.thecljvector.layout :as layout]
            [net.thegeez.thecljvector.time :as time]
            [net.thegeez.thecljvector.comments :as comments]))

(defmethod form/render-form-field :comments/hidden
  [{:keys [id label name] :as def} value errors]
  [:input
   {:type "hidden"
    :id name
    :name name
    :value value}])

(defn comment-box [context parent-id]
  [:div#comment-box
   (if-let [user-id (get-in context [:auth :id])]
     (form/html-form context {:action (get-in context [:response :data :links :comment :create])
                              :fields (form/form-fields :comment
                                                        comments/create-comment-form
                                                        (assoc (get-in context [:response :data :comment])
                                                               :parent-id parent-id))
                              :submit "Submit comment"})
     [:form
      [:div.form-group
       [:label.control-label {:for "comment[text]"} "Comment"]
       [:textarea.form-control
        {:id "comment[text]"
         :name "comment[text]"
         :disabled "disabled"}
        "You need to be logged in to comment"]]
      [:div.form-group
       [:input.btn.btn-primary
        {:type "submit"
         :disabled "disabled"
         :value "Submit comment"}]]])])

(defn render-comment [comment]
  (let [{:keys [id text username created_at]} comment]
    (assoc comment :html
           [:span
            {:id (str "comment-" id)}
            [:input {:name "comment-id"
                     :type "hidden"
                     :value id}]
            [:a {:name id}] username
            " "
            [:a {:href (str "#" id)
                 :onclick "javascript:highlightComment(); return true;"}
             (time/time-ago-tags created_at)]
            " "
            [:a {:href ""
                 :onclick
                 (str "javascript:
$('input[name=\"comment[parent-id]\"]').val('" id "');
var element = $('#comment-box').detach();
$('#comment-" id "').append(element);
$('input[name=\"comment[text]\"]').focus();
return false;")}
             "reply"]
            [:br]
            text
            #_(:path comment)])))

(defn flat->nest [flat]
  (let [flat (mapv (fn [x]
                     (let [path (:path x)
                           pieces (rest (string/split path #"\."))]
                       (assoc x :orig-path path :pieces pieces))) flat)
        f->n (fn f->n [flat]
               (let [branches (partition-by (comp first :pieces) flat)]
                 (into [:ul]
                       (mapv (fn [b]
                               (if (< 1 (count b))
                                 (let [b (mapv #(update-in % [:pieces] rest) b)
                                       [root nested] (split-with (comp empty? :pieces) b)]
                                   [:li (map :html root) (f->n nested)])
                                 [:li (map :html b)])) branches))))]
    (f->n flat)))

(defn comments-thread [context]
  [:div
   [:div#root-comment
    [:a {:href ""
         :onclick
         (str "javascript:
$('input[name=\"comment[parent-id]\"]').val('0');
var element = $('#comment-box').detach();
$('#root-comment').append(element);
$('input[name=\"comment[text]\"]').focus();
return false;")}
     "reply"]
    (comment-box context 0)]
   (flat->nest
    (map render-comment
         (get-in context [:response :data :post :comments])))])

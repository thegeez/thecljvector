(ns net.thegeez.thecljvector.comments
  (:require [clojure.java.jdbc :as jdbc]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.log :as log]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.link :as link]))

(def create-comment-form
  [{:id :post-id
    :type :hidden
    :render :comments/hidden}
   {:id :parent-id
    :type :hidden
    :label "reply-to"
    :render :comments/hidden}
   {:id :text
    :label "Comment"
    :coerce (fn [comment]
              (when (seq comment)
                comment))
    :type :string
    :render :post/textarea
    :validator (fn [comment]
                 (when (not (seq comment))
                   "Comment can't be empty"))}])

(defn fail-context [context error-msg]
  (let [post-id (get-in context [:request :data :comment :post-id])
        location (link/link context :posts/show :params {:id post-id})]
    (merge context
                 {:response
                  {:status 303
                   :headers {"Location" location}
                   :flash {:error (str
                                   error-msg
                                   " Your comment was: "
                                   (get-in context [:request :data :comment :text]))}}})))

(def push-chars (into [] "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz"))

(defn diff-stamp [then now]
  (let [then (long (/ then 1000)) ;; milli to seconds
        now (long (/ now 1000))
        diff (- now then)
        diff (+ diff 10000000000) ;; pad so every encoding is the same size, will break after ~300 years
        ;; make lexical sorted
        chs (loop [diff diff
                   timestamp-chars '()]
                (let [char (get push-chars (mod diff 64))
                      timestamp-chars (conj timestamp-chars char)]
                  (if (zero? diff)
                    (apply str timestamp-chars)
                    (let [diff (long (Math/floor (/ diff 64)))]
                      (recur diff
                             timestamp-chars)))))]
    chs))

(defn insert-comment [database values]
  (let [parent-id (try (let [parent-id (get values :parent-id)]
                         (Long/parseLong parent-id))
                       (catch Exception _ nil))
        _ (assert parent-id)
        post-id (try (let [post-id (get values :post-id)]
                       (Long/parseLong post-id))
                     (catch Exception _ nil))
        _ (assert post-id)
        [parent path] (or (when-let [parent-comment (first (jdbc/query database ["Select * from comments where id = ?" parent-id]))]
                            [parent-comment (:path parent-comment)])
                          (let [parent-post (first (jdbc/query database ["Select * from posts where id = ?" post-id]))]
                            [parent-post post-id]))
        comment-path (some (fn [comment-path]
                             (when (not (first (jdbc/query database ["Select * from comments where post_id = ? AND path = ?" post-id comment-path])))
                               comment-path))
                           (repeatedly (fn []
                                         (str path "." (diff-stamp (:created_at parent)
                                                                   (.getTime (java.util.Date.)))))))
        hidden (:hidden (first (jdbc/query database ["select u.hidden from users u where u.id = ?" (:user_id values)])))]
    (try
      (jdbc/insert! database
                    :comments
                    {:post_id post-id
                     :text (:text values)
                     :user_id (:user_id values)
                     :hidden hidden
                     :path comment-path
                     :created_at (.getTime (java.util.Date.))
                     :updated_at (.getTime (java.util.Date.))})
      (catch Exception e
        (println "e" e)
        {:error "Could not insert post to database"}))))

(def create
  (interceptor/interceptor
   {:enter (fn [context]
             (if-let [errors (get-in context [:request :data :comment :errors])]
               (fail-context context "Creating comment failed")
               (let [values (-> (get-in context [:request :data :comment])
                                (assoc :user_id (get-in context [:auth :id])))
                     res (insert-comment (:database context) values)]
                 (if-let [error (:error res)]
                   (fail-context context error)
                   (let [post-id (get-in context [:request :data :comment :post-id])
                         location (link/link context :posts/show :params {:id post-id})]
                     (merge context
                            {:response
                             {:status 201
                              :headers {"Location" location}
                              :flash {:info "Comment added"}}}))))))}))

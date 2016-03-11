(ns net.thegeez.thecljvector.posts
  (:require [clojure.java.jdbc :as jdbc]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.log :as log]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.link :as link]))

(defn post-resource [context post]
  (let [{:keys [id text link comment_count username]} post
        type (if text
               :self-post
               :link)
        self-link (link/link context :posts/show :params {:id id})
        user-link (link/link context :users/show :params {:username username})]
    (-> post
        (assoc :type type)
        (assoc :links {:self self-link
                       :user user-link})
        (assoc :link (if (= type :self-post)
                       self-link
                       link))
        (assoc :post-link self-link)
        (cond-> (= type :link)
          (assoc :host link))
        (assoc :comment-count comment_count))))

(defn get-posts [context]
  (let [db (:database context)
        results (jdbc/query db ["SELECT p.*, u.username, (SELECT COUNT(c.id) FROM comments c WHERE c.post_id = p.id) AS comment_count FROM posts p JOIN users u ON u.id = p.user_id WHERE p.hidden = FALSE ORDER BY p.created_at DESC"])]
    (mapv (partial post-resource context) results)))

(defn get-post [context id]
  (let [db (:database context)
        post (first (jdbc/query db ["SELECT p.*, u.username, (SELECT COUNT(c.id) FROM comments c WHERE c.post_id = p.id) AS comment_count FROM posts p JOIN users u ON u.id = p.user_id AND p.id = ?" id]))]
    (when post
      (let [comments (->> (jdbc/query db ["SELECT c.*, u.username FROM comments c JOIN users u ON u.id = c.user_id AND c.post_id = ? ORDER BY c.path" id])
                          (mapv (fn [comment]
                                  (if (:hidden comment)
                                    (assoc comment :text "[hidden]")
                                    comment))))
            post (assoc post :comments comments)]
        (post-resource context post)))))

(def with-post
  (interceptor/interceptor
   {:enter (fn [context]
             (if-let [post (get-post context (get-in context [:request :path-params :id]))]
               (merge context
                      {:post post
                       :comment {:post-id (:id post)}})
               (context/terminate context 404)))}))

(defn new-post [context]
  {:username (get-in context [:auth :username])})

(defn post-links [context]
  {:create (link/link context :posts/create)
   :comment {:create (link/link context :comments/create)}})

(def show
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:post (:post context)
                             :comment (:comment context)
                             :links (post-links context)}}}))}))

(def create-post-form
  [{:id :username
    :label "Posting as:"
    :type :static}
   {:id :title
    :label "Title"
    :coerce (fn [title]
              (when (seq title)
                title))
    :type :string}
   {:id :link
    :label "Link"
    :coerce (fn [link]
              (when (seq link)
                link))
    :type :string}
   {:id :text
    :label "Text"
    :type :text
    :coerce (fn [text]
              (when (seq text)
                text))
    :render :post/textarea}])

(defn get-recent-post-time [context]
  (let [id (get-in context [:auth :id])
        latest (:latest (first (jdbc/query (:database context)
                                           ["SELECT created_at as latest FROM posts p WHERE user_id = ? ORDER BY created_at DESC" id])))
        day-ago (- (.getTime (java.util.Date.)) (* 24 60 60 1000))]
    (when (and latest
               (< day-ago latest))
      (java.util.Date. latest))))

(def post-rate-limiter
  (interceptor/interceptor
   {:enter (fn [context]
             (if-let [recent-post-time (get-recent-post-time context)]
               (-> (merge context
                          {:response
                           {:status 200
                            :data {:recent-post-time recent-post-time}}})
                   context/terminate)
               context))}))

(def new
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:post (new-post context)
                             :links (post-links context)}}}))}))

(defn fail-context [context error-msg]
  (merge context
         {:response
          {:status 422
           :flash {:error error-msg}
           :data {:post (merge (new-post context)
                               (get-in context [:post])
                               (get-in context [:request :data :post]))
                  :links (post-links context)}}}))

(defn validate-create-post [post]
  (let [{:keys [title link text]} post
        errors (cond
                 (not (seq title))
                 {:title ["Title can't be empty"]}
                 (and (not (seq link))
                      (not (seq text)))
                 (let [msg "Supply a link or type some text"]
                   {:link [msg]
                    :text [msg]})
                 (and (seq link) (seq text))
                 (let [msg "You can only submit a link or a text post"]
                   {:link [msg]
                    :text [msg]})
                 (and link
                      (not (seq text)))
                 (cond
                   (not (seq link))
                   {:link ["Link can't be empty"]}
                   (not (or (.startsWith link "http://")
                            (.startsWith link "https://")))
                   {:link ["Link needs to start with http:// or https://"]})
                 (and (not link)
                      text)
                 (cond
                   (not (seq text))
                   {:text ["Text can't be empty"]}))]
    errors))

(defn insert-post [database values]
  (try
    (let [hidden (:hidden (first (jdbc/query database ["select u.hidden from users u where u.id = ?" (:user_id values)])))]
      (jdbc/insert! database
                    :posts
                    (merge values
                           {:hidden hidden
                            :created_at (.getTime (java.util.Date.))
                            :updated_at (.getTime (java.util.Date.))})))
    (catch Exception _
      {:error "Could not insert post to database"})))

(def create
  (interceptor/interceptor
   {:enter (fn [context]
             (if-let [errors (validate-create-post (get-in context [:request :data :post]))]
               (fail-context
                (assoc-in context [:request :data :post :errors]
                          errors)
                "Creating post failed")
               (let [values (-> (get-in context [:request :data :post])
                                (assoc :user_id (get-in context [:auth :id])))
                     res (insert-post (:database context) values)]
                 (if-let [error (:error res)]
                   (fail-context context error)
                   (let [new-id (long (val (ffirst res)))
                         location (link/link context :posts/show :params {:id new-id})]
                     (merge context
                            {:response
                             {:status 201
                              :headers {"Location" location}
                              :flash {:info "Post created"}}}))))))}))

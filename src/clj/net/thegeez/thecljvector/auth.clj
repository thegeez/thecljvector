(ns net.thegeez.thecljvector.auth
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]
            [io.pedestal.interceptor :as interceptor]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.link :as link]
            [net.thegeez.thecljvector.users :as users]))

(def return-if-logged-in
  (interceptor/interceptor
   {:enter (fn [context]
             (if-let [user (get-in context [:auth])]
               (context/terminate
                (merge context
                       {:response {:status 303
                                   :headers {"Location" (link/next-or-link context :home)}
                                   :flash {:message "Already logged in"}}}))
               context))}))

(defn get-auth-by-id [db id]
  (first (jdbc/query db ["SELECT id, username, admin, created_at, updated_at FROM users WHERE id = ?" id])))

(defn get-auth-by-credentials [db values]
  (let [{:keys [username password]} values]
    (when-let [user (first (jdbc/query db ["SELECT * FROM users WHERE username = ?" username]))]
      (when (hashers/check password (:password_encrypted user))
        (dissoc user :password_encrypted)))))

(defn get-auth-user [context]
  (when-let [id (get-in context [:request :session :auth :id])]
    (when-let [auth (get-auth-by-id (:database context) id)]
      (update-in auth [:links] merge {:self (link/link context :users/show :params {:username (:username auth)})
                                      :logout (link/link context :auth/logout :params {:next (link/self context)})}))))

(def with-auth
  (interceptor/interceptor
   {:enter (fn [context]
             (let [context (assoc context :login
                                  {:links
                                   {:login (link/link context :auth/login :params {:next (link/self context)})}})]
               (if-let [{:keys [id username] :as user} (get-auth-user context)]

                 (if-not username
                   ;; oauth flow needs to be completed
                   (if (= (get-in context [:request :path-info])
                          "/oauth/create-name")
                     context
                     (context/terminate
                      (merge context
                             {:response
                              {:status 303
                               :headers {"Location" (link/link context :auth/oauth-create-name :params {:next (link/self context)})}}})))
                   ;; user has complete profile with username
                   (assoc context :auth user))
                 context)))}))

(def valid-username-char? (set "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890_"))
(defn validate-username [username]
  (cond
    (not (seq username))
    "Username can't be empty"
    (try (Long/parseLong username)
         (catch Exception _ nil))
    "Username can't be a number"
    (some (complement valid-username-char?) username)
    "Username may only contain a-z, A-Z, a number or _"))

(def login-form
  [{:id :username
    :label "Username"
    :type :string
    :validator validate-username}
   {:id :password
    :label "Password"
    :type :password
    :validator (fn [password]
                 (when (not (seq password))
                   "Password can't be empty"))}])

(defn login-page-links [context]
  {:login (link/link-with-next context :auth/login-post)
   :signup (link/link-with-next context :auth/signup-post)
   :home (link/link context :home)
   :github (link/link context :auth/oauth-github
                      :params {:next (link/next-or-link context :home)})
   :google (link/link context :auth/oauth-google
                      :params {:next (link/next-or-link context :home)})
   :facebook (link/link context :auth/oauth-facebook
                        :params {:next (link/next-or-link context :home)})
   :twitter (link/link context :auth/oauth-twitter
                       :params {:next (link/next-or-link context :home)})
   })

(def login
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :session {:auth nil} ;; visit /login is auto logout
                      :data {:links (login-page-links context)}}}))}))

(def login-post
  (interceptor/interceptor
   {:enter (fn [context]
             (let [credentials (get-in context [:request :data :login-credentials])
                   fail-context (merge context
                                       {:response
                                        {:status 422
                                         :flash {:error "Login failed"}
                                         :data {:login-credentials credentials
                                                :links (login-page-links context)}}})]
               (if (:errors credentials)
                 fail-context
                 (if-let [auth (get-auth-by-credentials (:database context) credentials)]
                   (merge context
                          {:response
                           {:status 303
                            :headers {"Location"
                                      (link/next-or-link context :users/show :params {:id (:id auth)})}
                            :session {:auth {:id (:id auth)}}
                            :flash {:info "Login successful"}}})
                   fail-context))))}))

(def signup-form
  (into login-form
        [{:id :password-repeat
          :label "Password (repeat)"
          :type :password
          :validator (fn [password]
                       (when (not (seq password))
                         "Password (repeat) can't be empty"))}]))

(def signup-post
  (interceptor/interceptor
   {:enter (fn [context]
             (let [credentials (get-in context [:request :data :signup-credentials])
                   credentials (or (when (:errors credentials)
                                     credentials)
                                   (cond-> credentials
                                        (not= (:password credentials)
                                              (:password-repeat credentials))
                                        (->
                                         (assoc-in [:errors :password] ["Passwords do not match"])
                                         (assoc-in [:errors :password-repeat] ["Passwords do not match"]))))
                   fail-context (merge context
                                       {:response
                                        {:status 422
                                         :flash {:error "You can't sign up with these credentials"}
                                         :data {:signup-credentials credentials
                                                :links (login-page-links context)}}})]
               (if (:errors credentials)
                 fail-context
                 (let [id-or-errors (users/create-user context credentials)]
                   (if (:errors id-or-errors)
                     (update-in fail-context [:response :data :signup-credentials] merge id-or-errors)
                     (merge context
                            {:response
                             {:status 303
                              :headers {"Location"
                                        (link/next-or-link context :home)}
                              :session {:auth {:id id-or-errors}}
                              :flash {:info "Created username and you are now logged in"}}}))))))}))

(def logout-post
  (interceptor/interceptor
   {:leave (fn [context]
             (merge context
                    {:response
                     {:status 303
                      :headers {"Location" (link/next-or-link context :home)}
                      :session {:auth nil}
                      :flash {:info "Logout successful"}}}))}))

(def create-name-form
  [{:id :username
    :label "Username"
    :type :string
    :validator validate-username}])


(def create-name
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:create-name {:username (get-in context [:request :session :suggested-name])}
                             :links {:create-name (link/self context)}}}}))}))

(def create-name-post
  (interceptor/interceptor
   {:enter (fn [context]
             (let [create-name (get-in context [:request :data :create-name])
                   fail-context (merge context
                                       {:response
                                        {:status 422
                                         :flash {:error "Creating username failed"}
                                         :data {:create-name create-name
                                                :links {:create-name (link/link context :auth/oauth-create-name)}}}})]
               (if (:errors create-name)
                 fail-context
                 (let [id (get-in context [:request :session :auth :id])
                       _ (assert id)
                       username (:username create-name)
                       id-or-errors (users/set-name context id username)]
                   (if (:errors id-or-errors)
                     (update-in fail-context [:response :data :create-name] merge id-or-errors)
                     (merge context
                            {:response
                             {:status 303
                              :headers {"Location"
                                        (link/next-or-link context :home)}
                              :session {:auth {:id id}}
                              :flash {:info "Created username and you are now logged in"}}}))))))}))

(def require-authentication
  (interceptor/interceptor
   {:enter (fn [context]
             (if-not (:auth context)
               (-> (merge context
                          {:response
                           {:status 401
                            :headers {"Location" (link/link context :auth/login
                                                            :params {:next (link/self context)})}
                            :flash {:info "You need to be logged in for that action."}}})
                   context/terminate)
               context))}))

(def require-authorization
  (interceptor/interceptor
   {:enter (fn [context]
             (if (and (:auth context)
                      (:snippet context)
                      (= (:owner (:snippet context))
                         (:id (:auth context))))
               context
               (-> (merge context
                          {:response
                           {:status 403
                            :headers {"Location" (link/link context :auth/login
                                                            :params {:next (link/self context)})}
                            :flash {:info "You are not authorized to do that"}}})
                   context/terminate)))}))

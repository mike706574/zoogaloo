(ns zoogaloo.frontend.app
  (:require [cljs.pprint :as pprint]
            [clojure.string :as str]
            [day8.re-frame.http-fx]
            [zoogaloo.frontend.icons :as icons]
            [zoogaloo.frontend.storage :as storage]
            [zoogaloo.frontend.nav :as nav]
            [zoogaloo.frontend.request :as req]
            [zoogaloo.frontend.routing :as routing]
            [zoogaloo.frontend.table :as table]
            [zoogaloo.frontend.util :as util]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [re-frame.core :as rf]))

;; -- Development
(enable-console-print!)

;; -- Event Handlers --
(rf/reg-event-db
 :initialize
 (fn [db [_ account]]
   (merge db
          {:current-route nil
           :account account
           :loading false
           :error nil})))

;; Login
(defn login-request [params]
  (req/post-json-request {:uri "/api/tokens"
                          :params params
                          :on-success [:process-login]
                          :on-failure [:handle-login-failure]}))

(rf/reg-event-fx
 :login
 (fn [_ [_ params]]
   {:http-xhrio (login-request params)}))

(rf/reg-event-fx
 :process-login
 (fn [{db :db} [_ response]]
   (let [account (js->clj response)]
     {:db (assoc db :account account)
      :storage [[:set :account account]]
      :push-route [:home]})))

(rf/reg-event-db
 :handle-login-failure
 (fn [db [_ {:keys [status response] :as result}]]
   (if (= status 401)
     (assoc db :login-error (:error response))
     (assoc db :error result))))

;; Register
(defn register-request [params]
  (req/post-json-request {:uri "/api/accounts"
                          :params params
                          :on-success [:process-register]
                          :on-failure [:handle-register-failure]}))

(rf/reg-event-fx
 :register
 (fn [_ [_ params]]
   {:http-xhrio (register-request params)}))

(rf/reg-event-fx
 :process-register
 (fn [_ _]
   {:push-route [:login]}))

(rf/reg-event-db
 :handle-register-failure
 (fn [db [_ {:keys [status response] :as result}]]
   (if (= status 400)
     (assoc db :register-error (:error response))
     (assoc db :error result))))

;; Logout
(rf/reg-event-fx
 :logout
 (fn [{db :db} _]
   {:db (dissoc db :account)
    :storage [[:remove :account]]}))

;; Error handling
(rf/reg-event-db
 :handle-failure
 (fn [db [_ response]]
   (assoc db :error response :loading false)))

;; Routing
(rf/reg-event-fx
  :navigate
  (fn [_ [_ & route]]
    {:navigate! route}))

;; -- Subscriptions --

(rf/reg-sub
  :current-route
  (fn [db]
    (:current-route db)))

(rf/reg-sub
  :state
  (fn [db _]
    (select-keys db [:loading :error :account])))

(rf/reg-sub
  :login-error
  (fn [db _]
    (:login-error db)))

(rf/reg-sub
  :register-error
  (fn [db _]
    (:register-error db)))

(rf/reg-sub
  :account
  (fn [db _]
    (:account db)))

;; -- Views --
(defn register-page []
  (let [email-atom (r/atom "")
        password-atom (r/atom "")]
    (fn []
      (let [error @(rf/subscribe [:register-error])
            email @email-atom
            password @password-atom
            params {:email email :password password}
            email-taken? (= error "email-taken")
            disabled? (or (str/blank? email) (str/blank? password))]
        [:<>
         [:p "You're trying to register."]
         [:h2 "Register"]
         [:div.row.mb-3
          [:label.col-sm-2.col-form-label
           {:for "email"}
           "Email"]
          [:div.col-sm-4
           [:input.form-control
            {:id "email"
             :type "email"
             :name "email"
             :value email
             :auto-focus true
             :class (util/classes [["is-invalid" email-taken?]])
             :on-change #(reset! email-atom (-> % .-target .-value))}]
           (when email-taken? [:div.invalid-feedback "An account with this email already exists."])]]
         [:div.row.mb-3
          [:label.col-sm-2.col-form-label
           {:for "password"}
           "Password"]
          [:div.col-sm-4
           [:input.form-control
            {:id "password"
             :type "text"
             :name "password"
             :value password
             :style {"WebkitTextSecurity" "disc"}
             :on-change #(reset! password-atom (-> % .-target .-value))}]]]
         [:button.btn.btn-primary
          {:type "submit"
           :disabled disabled?
           :on-click #(rf/dispatch [:register params])}
          "Submit"]]))))

(defn login-page []
  (let [email-atom (r/atom "")
        password-atom (r/atom "")]
    (fn []
      (let [error @(rf/subscribe [:login-error])
            email @email-atom
            password @password-atom
            params {:email email :password password}
            missing-account? (= error "missing-account")
            invalid-password? (= error "invalid-password")
            disabled? (or (str/blank? email) (str/blank? password))]
        [:<>
         [:p "You're trying to log in."]
         [:h2 "Login"]
         [:div.row.mb-3
          [:label.col-sm-2.col-form-label
           {:for "email"}
           "Email"]
          [:div.col-sm-4
           [:input.form-control
            {:id "email"
             :type "email"
             :name "email"
             :value email
             :auto-focus true
             :class (util/classes [["is-invalid" missing-account?]])
             :on-change #(reset! email-atom (-> % .-target .-value))}]
           (when missing-account? [:div.invalid-feedback "Account not found."])]]
         [:div.row.mb-3
          [:label.col-sm-2.col-form-label
           {:for "password"}
           "Password"]
          [:div.col-sm-4
           [:input.form-control
            {:id "password"
             :type "password"
             :name "password"
             :value password
             :class (util/classes [["is-invalid" invalid-password?]])
             :on-change #(reset! password-atom (-> % .-target .-value))}]
           (when invalid-password? [:div.invalid-feedback "Invalid password."])]]
         [:button.btn.btn-primary
          {:type "submit"
           :disabled disabled?
           :on-click #(rf/dispatch [:login params])}
          "Submit"]]))))

(defn home-page []
  [:<>
   [:p "This is a home page."]])

(defn app []
  (let [{:keys [loading error account]} @(rf/subscribe [:state])
        current-route @(rf/subscribe [:current-route])]
    [:div.container
     {:style {"marginTop" "1em"}}
     [:div.row
      [:div.col.auto
       [:a {:href "/"}
        [:h1 "zoogaloo"]]]
      [:div.col-md-4
       (if account
         [:div.float-end
          (:email account)
          [:button.btn.btn-secondary.ms-2
           {:type "button"
            :style {"display" "inline"}
            :on-click #(rf/dispatch [:logout])}
           "Logout"]]
         [:div.float-end
          [:a {:href (routing/href :login)} "Login"]
          [:a.ms-2 {:href (routing/href :register)} "Register"]])]]
     (cond
       loading [:p "Loading..."]
       error [:<>
              [:p "An error occurred."]
              (if (string? error)
                [:p error]
                [:pre (with-out-str (pprint/pprint error))])]
       :else (when current-route
               [(-> current-route :data :view)]))]))

;; -- Routes --

(def routes
  ["/"
   [""
    {:name :home
     :view home-page
     :controllers
     [{:start (fn [] (println "Entering home page"))
       :stop (fn [] (println "Leaving home page"))}]}]

   ["register"
    {:name :register
     :view register-page
     :controllers
     [{:start (fn [] (println "Entering register page"))
       :stop (fn [] (println "Leaving register page"))}]}]

   ["login"
    {:name :login
     :view login-page
     :controllers
     [{:start (fn [] (println "Entering login page"))
       :stop (fn [] (println "Leaving login page"))}]}]])

;; -- Entry Point--

(defn init []
  (println "Initializing")
  (let [account (storage/get-account)]
    (rf/dispatch-sync [:initialize account])
    (routing/init-routes! routes)
    (rd/render [app] (js/document.getElementById "app"))))

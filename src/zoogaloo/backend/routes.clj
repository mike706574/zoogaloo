(ns zoogaloo.backend.routes
  (:require [zoogaloo.backend.auth :as auth]
            [zoogaloo.backend.middleware :as mw]
            [zoogaloo.backend.repo :as repo]
            [zoogaloo.backend.schema :as s]
            [reitit.swagger :as swagger]
            [ring.util.response :as resp]
            [taoensso.timbre :as log]))

(def email-and-password-params
  [:map {:closed true}
   [:email string?]
   [:password string?]])

(def uuid-params
  [:map {:closed true}
   [:uuid string?]])

(defn routes
  [{:keys [auth db]}]
  [["/" {:get {:no-doc true
               :handler (fn [_] (resp/resource-response "public/index.html"))}}]

   ["/swagger.json" {:get {:no-doc true
                           :swagger {:info {:title "zoogaloo api"}}
                           :handler (swagger/create-swagger-handler)}}]

   ["/api" {:middleware [(mw/auth auth)]}
    ["/accounts" {:get {:parameters {}
                        :responses {200 {:body [:sequential s/account-model]}}
                        :handler (fn [_]
                                   {:status 200
                                    :body (repo/list-accounts db)})}

                  :post {:parameters {:body email-and-password-params}
                         :responses {200 {:body [:map {:closed true}
                                                 [:email string?]]}}
                         :handler (fn [{{{:keys [email password]} :body} :parameters}]
                                    (let [{:keys [status]} (auth/register-account auth email password)]
                                      (case status
                                        "registered" {:status 200 :body {:email email}}
                                        "email-taken" {:status 400 :body {:error "email-taken"}})))}}]

    ["/tokens" {:post {:parameters {:body email-and-password-params}
                       :responses {200 {:body any?}}
                       :handler (fn [{{{:keys [email password]} :body} :parameters}]
                                  (let [{:keys [status account token]} (auth/generate-token auth email password)]
                                    (if (= status "generated")
                                      {:status 200 :body (assoc account :token token)}
                                      {:status 401 :body {:error status}})))}}]]])

(ns zoogaloo.common.client
  (:require [aleph.http :as http]
            [zoogaloo.common.json :as json]))

(defn- get-request [{:keys [url query-params headers]}]
  (let [{:keys [status body]} @(http/get url {:query-params query-params
                                              :headers headers})]
    (when (= 200 status)
      (json/read-value body))))

(defn- post-request [{:keys [url body headers]}]
  (let [{:keys [status body]} @(http/post url {:body (json/write-value-as-string body)
                                               :headers (assoc headers "Content-Type" "application/json")})]
    (when (= 200 status)
      (json/read-value body))))

(defn- get-auth-headers [{:keys [url email password]}]
  (if email
    (let [token (:token (post-request {:url (str url "/api/tokens")
                                       :body {:email email :password password}}))]
      {"Authorization" (str "Token " token)})
    {}))

(defprotocol SystemClient
  (register [this email password]))

(defrecord ApiSystemClient [url email password]
  SystemClient
  (register [_ new-email new-password]
    (post-request {:url (str url "/api/accounts")
                   :body {:email new-email :password new-password}})))

(defn new-client
  [config]
  (map->ApiSystemClient config))

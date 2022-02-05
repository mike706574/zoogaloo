(ns zoogaloo.cli.config
  (:require [zoogaloo.common.client :as client]
            [zoogaloo.common.config :as config]))

(defn config
  ([]
   (config {:env (config/get-env)
            :password (config/get-env-var "")}))

  ([{:keys [env password]}]
   {:client (case env
              "dev" {:url "http://localhost:8080"
                     :email "admin"
                     :password password}
              "prod" {:url "https://zoogaloo-mike.herokuapp.com"
                      :email "admin"
                      :password password})}))

(defn deps [config]
  {:client (client/new-client (:client config))})

(ns zoogaloo.backend.config
  (:require [zoogaloo.common.config :as config]))

(def port-var "PORT")

(defn get-port []
  (Integer. (or (config/get-env-var port-var) 8080)))

(def admin-password-var "ADMIN_PASSWORD")

(defn get-admin-password []
  (or (config/get-env-var admin-password-var)
      (throw (ex-info "Admin password environment variable not set."
                      {:var admin-password-var}))))

(def secret-var "SECRET")

(defn get-secret []
  (or (config/get-env-var secret-var)
      (throw (ex-info "Secret environment variable not set."
                      {:var secret-var}))))

(defn config
  ([]
   (config {:admin-password (get-admin-password)
            :env (config/get-env)
            :port (get-port)}))
  ([{:keys [admin-password env port]}]
   {:port port
    :auth {:admin-password admin-password
           :secret (case env
                     "dev" "secret"
                     "prod" (get-secret)
                     (throw (ex-info "Invalid env" {:env env})))}
    :db (case env
          "dev" {:dbtype "postgresql"
                 :classname "org.postgresql.Driver"
                 :subprotocol "postgres"
                 :host "localhost"
                 :port 5432
                 :dbname "postgres"
                 :user "postgres"
                 :password "postgres"
                 :log? false}
          "prod" {:jdbcUrl (or (config/get-env-var "JDBC_DATABASE_URL")
                               (throw (ex-info "JDBC_DATABASE_URL not set" {})))}
          (throw (ex-info "Invalid env" {:env env})))}))

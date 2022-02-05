(ns user
  (:require [aleph.http :as http]
            [buddy.hashers :as auth-hashers]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.namespace.repl :as repl]
            [com.stuartsierra.component :as component]
            [zoogaloo.common.client :as client]
            [zoogaloo.common.config :as config]
            [zoogaloo.backend.db :as db]
            [zoogaloo.backend.repo :as repo]
            [zoogaloo.backend.system :as system]
            [zoogaloo.backend.config :as backend-config]
            [zoogaloo.cli.config :as cli-config]
            [zoogaloo.cli.core :as core]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [taoensso.timbre :as log]))

(repl/disable-reload!)

(repl/set-refresh-dirs "src")

(def port 7600)

;; backend
(def admin-password "admin!")
(def hashed-admin-password (auth-hashers/derive admin-password))

(def config (backend-config/config {:admin-password hashed-admin-password
                                    :env "dev"
                                    :port port}))

(defonce system nil)

(defn init []
  (alter-var-root #'system (constantly (system/system config)))
  :init)

(defn start []
  (try
    (alter-var-root #'system component/start-system)
    :started
    (catch Exception ex
      (log/error (or (.getCause ex) ex) "Failed to start system.")
      :failed)))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop-system s))))
  :stopped)

(defn go []
  (init)
  (start)
  :ready)

(defn reset []
  (stop)
  (repl/refresh :after `go))

(defn restart []
  (stop)
  (go))

(def db (db/new-db (:db config)))

(def cli-config (cli-config/config {:env "dev" :password admin-password}))
(def cli-deps (cli-config/deps cli-config))

(def client (client/new-client (:client cli-config)))


(comment
  ;; db
  (db/migrate db)
  (db/rollback db)
  (db/list-views db)
  (db/collect-tables db)
  (db/migrations prod-db)
  (db/reset db)

  (db/list-columns db)
  )

(def prod-db
  (jdbc/get-datasource (config/get-env-var "JDBC_DATABASE_URL")))

(def prod-cli-config (cli-config/config {:env "prod" :password (config/get-env-var "ADMIN_PASSWORD")}))

(def prod-deps (cli-config/deps prod-cli-config))

(comment

  )

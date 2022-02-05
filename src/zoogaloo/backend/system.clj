(ns zoogaloo.backend.system
  (:require [zoogaloo.backend.auth :as auth]
            [zoogaloo.backend.db :as db]
            [zoogaloo.backend.handler :as handler]
            [zoogaloo.backend.service :as service]))

(defn system
  [config]
  {:auth (auth/new-instance (:auth config))
   :db (db/new-db (:db config))
   :handler-factory (handler/new-factory config)
   :service (service/new-service config)})

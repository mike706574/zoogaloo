(ns zoogaloo.backend.repo
  (:require [zoogaloo.backend.db :as db]
            [next.jdbc :as jdbc]))

(def account-fields [:email :created])

(defn get-account-with-password [db keys]
  (first (db/select-items db :account {:keys keys
                                       :cols (conj account-fields :password)})))

(defn get-account [db keys]
  (first (db/select-items db :account {:keys keys
                                       :cols account-fields})))

(defn list-accounts [db]
  (db/select-items db :account {:cols account-fields
                                :order-by [:email]}))

(defn insert-account! [db account]
  (db/insert-item! db :account account))

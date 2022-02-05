(ns zoogaloo.backend.schema
  (:require [malli.util :as mu]))

(def audit-info
  [:map [:created inst?]])

(defn with-audit [schema]
  (mu/union schema audit-info))

(def account-model
  (with-audit [:map [:email string?]]))

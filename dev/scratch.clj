(ns scratch
  (:require [aleph.http :as http]
            [buddy.hashers :as auth-hashers]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.namespace.repl :as repl]
            [com.stuartsierra.component :as component]
            [malli.core :as m]
            [malli.generator :as mg]
            [malli.util :as mu]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [taoensso.timbre :as log]
            [zoogaloo.common.client :as client]
            [zoogaloo.common.config :as config]
            [zoogaloo.backend.db :as db]
            [zoogaloo.backend.repo :as repo]
            [zoogaloo.backend.system :as system]
            [zoogaloo.backend.config :as backend-config]
            [zoogaloo.cli.config :as cli-config]
            [zoogaloo.cli.core :as core]))

(defn get-path [row path]
  (if (seq? path)
    (get-in row path)
    (get row path)))

(defmulti get-formatter :type)

(defmethod get-formatter "numeric" [{:keys []}]

  )

(defmethod get-formatter :default [format]
  (throw (ex-info "invalid format type" format)))


(defn get-value [idx row col]
  (let [{:keys [resolve render format]} col]
    (cond
      render (render {:idx idx :row row})
      resolve (let [raw-value (cond
                                (seq? resolve) (get-in row resolve)
                                (keyword? resolve) (get row resolve)
                                (fn? resolve) (resolve {:idx idx :row row})
                                :else (throw (ex-info "invalid resolve" col)))
                    value (cond
                            (nil? format) raw-value
                            (fn? format) (format raw-value)
                            (map? )
                            :else (throw (ex-info "invalid format" col)))]
                value)
      :else (throw (ex-info "invalid column" {:col col})))))

(defn responsive-table [{:keys [cols rows row-key style] :as args}]
  (when-not row-key
    (throw (ex-info "row-key is required" args)))
  [:div.table-responsive
   {:style (merge {"padding" "1rem"
                   "border" "1px solid #dee2e6"}
                  style)}
   [:table.table
    [:thead
     [:tr
      (for [{:keys [key label] :as col} cols]
        [:th
         {:key key
          :scope "col"}
         label])]]
    [:tbody
     (for [[idx row] (map-indexed vector rows)]
       [:tr
        {:key (get-path row row-key)}
        (for [{key :key :as col} cols]
          [:td
           {:key key}
           (get-value idx row col)])])]]])

(def name-col
  {:key "name"
   :resolve :name})


(responsive-table
 {:cols [name-col]
  :rows [{:name "Bob"}
         {:name "Alice"}]
  :row-key :name})



(comment
  ;; format

  ;; column
  {:key "name" ;; string
   :label ;; string
   :renderLabel ;; function taking {:idx idx :col col :deps deps} returning anything
   :resolve ;; string, path, or function taking {:id idx :row row :col col :deps deps} returning primitive
   :render ;; function taking {:idx idx :row row :value value :col col :deps deps} returning anything
   :format ;; function taking {:idx idx :row row :value value :col col :deps deps} returning primitive
   :sort ;; boolean or function taking {:id idx :row row :col col :deps deps} returning boolean
   :filter ;; boolean or function taking {:id idx :row row :col col :deps deps}  returning boolean
   :export ;; boolean, defaults to true
   }


  )

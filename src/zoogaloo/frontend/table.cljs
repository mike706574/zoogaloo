(ns zoogaloo.frontend.table)

(defn get-path [row path]
  (if (seq? path)
    (get-in row path)
    (get row path)))

(def number-format (js/Intl.NumberFormat. "en-us"))
(defn format-number [{:keys [val]}]
  (.format number-format val))

(def currency-format (js/Intl.NumberFormat. "en-us" (js-obj "style" "currency" "currency" "USD")))
(defn format-currency [{:keys [val]}]
  (.format currency-format val))

(defmulti get-formatter :type)

(defmethod get-formatter :numeric [format]
  format-number)

(defmethod get-formatter :currency [format]
  format-currency)

(defmethod get-formatter :default [format]
  (throw (ex-info "invalid format type" format)))

(defn get-val [idx row col]
  (let [{:keys [resolve render format]} col]
    (cond
      render (render {:idx idx :row row})
      resolve (let [raw-val (cond
                                (seq? resolve) (get-in row resolve)
                                (keyword? resolve) (get row resolve)
                                (fn? resolve) (resolve {:idx idx :row row})
                                :else (throw (ex-info "invalid resolve" col)))
                    val (cond
                            (nil? format) raw-val
                            (fn? format) (format raw-val)
                            (map? format) ((get-formatter format) {:val raw-val})
                            :else (throw (ex-info "invalid format" col)))]
                val)
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
           (get-val idx row col)])])]]])

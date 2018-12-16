(ns meo.jvm.graphql.custom-fields
  (:require [meo.jvm.graph.query :as gq]
            [taoensso.timbre :refer [info error warn debug]]
            [matthiasn.systems-toolbox.component :as stc]
            [ubergraph.core :as uber]
            [meo.jvm.datetime :as dt]
            [clj-time.format :as ctf]
            [clj-time.core :as ct]
            [meo.jvm.graphql.common :as gc]))

(defn custom-fields-cfg
  "Generates the custom custom fields config map as required by the
   user interface. The usage of custom fields in the UI predates the
   definition of custom fields in a specialized entry. The data
   format should be adjusted subsequently."
  [state]
  (debug "custom-fields-cfg")
  (let [q {:tags #{"#custom-field-cfg"}
           :n    Integer/MAX_VALUE}
        res (:entries-list (gq/get-filtered state q))
        f (fn [entry]
            (let [{:keys [tag items pvt]} (:custom_field_cfg entry)
                  story (:primary_story entry)
                  fm (fn [field]
                       (let [k (keyword (:name field))
                             label (:label field)]
                         [k {:cfg   field
                             :label label}]))
                  fields (into {} (map fm items))]
              [tag {:default-story story
                    :timestamp     (:timestamp entry)
                    :pvt           pvt
                    :fields        fields}]))
        res (->> (map f res)
                 (sort-by #(:timestamp (second %)))
                 (filter first)
                 reverse
                 (into {}))]
    (debug "custom-fields-cfg" res)
    res))


(def dtz (ct/default-time-zone))
(def fmt (ctf/formatter "yyyy-MM-dd'T'HH:mm" dtz))
(defn parse [dt] (ctf/parse fmt dt))


(defn val-mapper [k field entry]
  (let [path [:custom_fields k field]
        ts (or (:adjusted_ts entry)
               (:timestamp entry))]
    {:v  (get-in entry path)
     :ts ts}))

(defn stats-mapper [tag nodes [k fields]]
  (let [field-mapper
        (fn [[field v]]
          (let [op (when (contains? #{:number :time} (:type (:cfg v)))
                     (case (:agg (:cfg v))
                       :min #(when (seq %) (apply min (map :v %)))
                       :max #(when (seq %) (apply max (map :v %)))
                       :sum #(apply + (map :v %))
                       #(when (seq %) (double (/ (apply + (map :v %)) (count %))))))
                res (vec (filter #(:v %) (mapv (partial val-mapper k field) nodes)))]
            [field {:v   (if op
                           (try (op res)
                                (catch Exception e (error e res)))
                           res)
                    :vs  res
                    :tag tag}]))]
    (into {} (mapv field-mapper fields))))

(defn adjusted-ts-filter [date-string entry]
  (let [adjusted-ts (:adjusted_ts entry)
        tz (:timezone entry)]
    (or (not adjusted-ts)
        (= (dt/ts-to-ymd-tz adjusted-ts tz)
           date-string))))

(defn fields-mapper [[k {:keys [v tag vs]}]]
  {:field  (name k)
   :tag    tag
   :value  v
   :values vs})

(defn custom-fields-mapper
  "Creates mapper function for custom field stats. Takes current state. Returns
   function that takes date string, such as '2016-10-10', and returns map with
   results for the defined custom fields, plus the date string. Performs
   operation specified for field, such as sum, min, max."
  [current-state tag]
  (let [custom-fields (custom-fields-cfg current-state)
        fields-def (into {} (map (fn [[k v]] [k (:fields v)])
                                 (select-keys custom-fields [tag])))
        g (:graph current-state)]
    (fn [date-string]
      (let [day-nodes (gq/get-nodes-for-day g {:date_string date-string})
            day-nodes-attrs (map #(uber/attrs g %) day-nodes)
            nodes (filter :custom_fields day-nodes-attrs)
            nodes (filter (partial adjusted-ts-filter date-string) nodes)
            fields (mapv (partial stats-mapper tag nodes) fields-def)]
        (apply merge
               {:date_string date-string
                :fields      (mapv fields-mapper (first fields))
                :tag         tag}
               fields)))))

(defn custom-field-stats [state context args value]
  (let [{:keys [days tag]} args
        days (reverse (range days))
        now (stc/now)
        custom-fields-mapper (custom-fields-mapper @state tag)
        day-strings (mapv #(dt/ts-to-ymd (- now (* % gc/d))) days)
        stats (mapv custom-fields-mapper day-strings)]
    stats))
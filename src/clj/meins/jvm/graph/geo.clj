(ns meins.jvm.graph.geo
  (:require [meins.jvm.graph.query :as gq]
            [meins.jvm.graphql.xforms :as xf]
            [taoensso.timbre :refer [info error warn debug]]
            [geo [spatial :as sp]])
  (:import [org.locationtech.spatial4j.shape.impl RectangleImpl]))

(defn photos-within-bounds [state context args value]
  (let [{:keys [ne_lat ne_lon sw_lat sw_lon]} args
        n Integer/MAX_VALUE
        res (gq/get-filtered @state {:tags #{"#photo"} :n n})
        entries (vals (:entries-map res))
        ne (sp/point ne_lat ne_lon)
        sw (sp/point sw_lat sw_lon)
        rect (RectangleImpl. sw ne sp/earth)
        nearby (fn [entry]
                 (let [{:keys [latitude longitude img_file]} entry]
                   (when (and latitude longitude img_file)
                     (let [point (sp/point latitude longitude)]
                       (sp/intersects? rect point)))))
        res (mapv xf/edn-xf (filter nearby entries))]
    res))

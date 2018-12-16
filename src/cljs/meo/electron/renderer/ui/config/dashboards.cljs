(ns meo.electron.renderer.ui.config.dashboards
  (:require [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [taoensso.timbre :refer-macros [info error]]
            [meo.electron.renderer.helpers :as h]
            [clojure.string :as s]
            [reagent.core :as r]
            [moment]
            [meo.electron.renderer.graphql :as gql]
            [meo.common.utils.misc :as m]
            [meo.electron.renderer.ui.entry.utils :as eu]
            [meo.electron.renderer.ui.journal :as j]))

(defn lower-case [str]
  (if str (s/lower-case str) ""))

(defn gql-query [pvt search-text put-fn]
  (let [queries [[:habits_cfg
                  {:search-text search-text
                   :n           1000}]]
        query (gql/tabs-query queries false pvt)]
    (put-fn [:gql/query {:q        query
                         :id       :habits_cfg
                         :res-hash nil
                         :prio     11}])))

(defn dashboard-line [_habit local put-fn]
  (let [show-pvt (subscribe [:show-pvt])
        cfg (subscribe [:cfg])]
    (fn habit-line-render [entry local put-fn]
      (let [ts (:timestamp entry)
            text (eu/first-line entry)
            text (or (when-not (empty? text)
                       text)
                     "YOUR DASHBOARD DESCRIPTION HERE")
            locale (:locale @cfg :en)
            date-str (h/localize-date (moment (or ts)) locale)
            sel (:selected @local)
            line-click (fn [_]
                         (swap! local assoc-in [:selected] ts)
                         (gql-query @show-pvt (str ts) put-fn))
            pvt (get-in entry [:dashboard_cfg :pvt])
            active (get-in entry [:dashboard_cfg :active])]
        [:tr {:key      ts
              :class    (when (= sel ts) "active")
              :on-click line-click}
         [:td date-str]
         [:td.habit text]
         [:td [:i.fas {:class (if active "fa-toggle-on" "fa-toggle-off")}]]
         [:td [:i.fas {:class (if pvt "fa-toggle-on" "fa-toggle-off")}]]]))))

(defn dashboards [local put-fn]
  (let [pvt (subscribe [:show-pvt])
        input-fn (fn [ev]
                   (let [text (lower-case (h/target-val ev))]
                     (swap! local assoc-in [:search] text)))
        open-new (fn [x]
                   (let [ts (:timestamp x)]
                     (swap! local assoc-in [:selected] ts)
                     (gql-query @pvt (str ts) put-fn)))
        add-click (h/new-entry put-fn {:entry_type    :dashboard_cfg
                                       :perm_tags     #{"#dashboard-cfg"}
                                       :tags          #{"#dashboard-cfg"}
                                       :dashboard_cfg {:active true}}
                               open-new)
        gql-res2 (subscribe [:gql-res2])
        pvt (subscribe [:show-pvt])
        by-ts #(get-in % [:timestamp])
        by-text #(get-in % [:text])
        by-pvt #(get-in % [:dashboard_cfg :pvt])
        by-active #(get-in % [:dashboard_cfg :active])]
    (fn dashboards-render [local put-fn]
      (let [pvt @pvt
            search-text (:search @local "")
            sort-fn (get-in @local [:dashboards_cfg :sorted-by] by-ts)
            sort-click (fn [f]
                         (fn [_]
                           (if (= f sort-fn)
                             (swap! local update-in [:dashboards_cfg :reverse] not)
                             (swap! local assoc-in [:dashboards_cfg :sorted-by] f))))
            pvt-filter (fn [x] (if pvt true (not (get-in x [1 :dashboard_cfg :pvt]))))
            search-match #(h/str-contains-lc?
                            (eu/first-line (second %))
                            (str search-text))
            dashboards (->> @gql-res2
                            :dashboard_cfg
                            :res
                            (filter pvt-filter)
                            (filter search-match))]
        [:div.col.habits
         [:h2 "Dashboards Editor"]
         [:div.input-line
          [:span.search
           [:i.far.fa-search]
           [:input {:on-change input-fn
                    :value     search-text}]
           [:span.add {:on-click add-click}
            [:i.fas.fa-plus]]]]
         [:table.habit_cfg
          [:tbody
           [:tr
            [:th {:on-click (sort-click by-ts)} "Created"]
            [:th {:on-click (sort-click by-text)} "Dashboard"]
            [:th {:on-click (sort-click by-active)} "active"]
            [:th {:on-click (sort-click by-pvt)} "private"]]
           (for [dashboard (vals dashboards)]
             ^{:key (:timestamp dashboard)}
             [dashboard-line dashboard local put-fn])]]]))))

(defn dashboards-tab [tab-group _put-fn]
  (let [query-cfg (subscribe [:query-cfg])
        query-id (reaction (get-in @query-cfg [:tab-groups tab-group :active]))
        search-text (reaction (get-in @query-cfg [:queries @query-id :search-text]))
        local-cfg (reaction {:query-id    @query-id
                             :search-text @search-text
                             :tab-group   tab-group})]
    (fn tabs-render [_tab-group put-fn]
      [:div.tile-tabs
       [j/journal-view @local-cfg put-fn]])))

(defn dashboards-row [local put-fn]
  [:div.habit-cfg-row
   [h/error-boundary
    [dashboards local put-fn]]
   (when (:selected @local)
     [h/error-boundary
      [dashboards-tab :habits_cfg put-fn]])])

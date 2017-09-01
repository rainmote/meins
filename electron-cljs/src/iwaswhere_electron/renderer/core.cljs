(ns iwaswhere-electron.renderer.core
  (:require [iwaswhere-electron.renderer.log]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [iwaswhere-electron.renderer.ipc :as ipc]
            [iwaswhere-electron.renderer.exec :as exec]
            [electron :refer [ipcRenderer]]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defonce switchboard (sb/component :renderer/switchboard))

(defn console-msg-handler [ev]
  (info "GUEST:" (.-message ev)))


(defn start []
  (info "Starting SYSTEM")
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :renderer/ipc-cmp)
                       (exec/cmp-map :renderer/exec-cmp #{:import/listen})}]

     [:cmd/route {:from :renderer/ipc-cmp
                  :to   #{:renderer/exec-cmp}}]

     [:cmd/route {:from :renderer/exec-cmp
                  :to   #{:renderer/ipc-cmp}}]

     [:cmd/send {:to  :renderer/exec-cmp
                 :msg [:exec/js "iwaswhere_web.ui.menu.hide()"]}]]))

(defn load-handler [ev]
  (info "RENDERER loaded")
  (let [webview (.querySelector js/document "webview")]
    (.addEventListener webview "console-message" console-msg-handler)
    (start)))


(.addEventListener js/window "load" load-handler)
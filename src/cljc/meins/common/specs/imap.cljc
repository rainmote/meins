(ns meins.common.specs.imap
  "Specs for sync via IMAP."
  (:require [meins.common.utils.parse :as p]
            #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])))


(s/def :sync/start-server nil?)
(s/def :sync/stop-server (s/nilable keyword?))
(s/def :sync/scan-inbox nil?)
(s/def :sync/scan-images nil?)


(s/def :meins.imap.server/host string?)
(s/def :meins.imap.server/password string?)
(s/def :meins.imap.server/user string?)
(s/def :meins.imap.server/authTimeout integer?)
(s/def :meins.imap.server/connTimeout integer?)
(s/def :meins.imap.server/port integer?)
(s/def :meins.imap.server/autotls boolean?)
(s/def :meins.imap.server/tls boolean?)

(s/def :meins.imap/server
  (s/keys :req-un [:meins.imap.server/host
                   :meins.imap.server/password
                   :meins.imap.server/user
                   :meins.imap.server/authTimeout
                   :meins.imap.server/connTimeout
                   :meins.imap.server/port
                   :meins.imap.server/autotls
                   :meins.imap.server/tls]))

(s/def :meins.imap.sync/mailbox string?)
(s/def :meins.imap.sync/secret string?)
(s/def :meins.imap.sync/last-read integer?)
(s/def :meins.imap.sync/body-part string?)

#_
(s/def :meins.imap.sync/read
  (s/keys :req-un [:meins.imap.sync/mailbox
                   :meins.imap.sync/secret
                   :meins.imap.sync/last-read
                   :meins.imap.sync/body-part]))

(s/def :meins.imap.sync/write
  (s/keys :req-un [:meins.imap.sync/mailbox
                   :meins.imap.sync/secret]))

(s/def :meins.imap/sync
  (s/keys :req-un [:meins.imap.sync/read
                   :meins.imap.sync/write]))

(s/def :imap/cfg
  (s/keys :req-un [:meins.imap/server
                   :meins.imap/sync]))

(s/def :imap/get-cfg nil?)

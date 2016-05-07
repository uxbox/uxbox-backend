;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.emails
  (:require [suricatta.core :as sc]
            [uxbox.persistence :as up]
            [uxbox.config :as cfg]
            [uxbox.sql :as sql]
            [uxbox.util.blob :as blob]
            [uxbox.util.transit :as t]
            [uxbox.util.emails :as emails]))

(def register-email
  #(emails/render "register" % {:reply-to "no-reply@uxbox.io"}))

(defn send!
  ([email] (send! email 5))
  ([email priority]
   (let [defaults (:email cfg/config)
         email (merge defaults email)
         data (-> email t/encode blob/encode)
         sqlv (sql/insert-email {:data data
                                 :priority priority})]
     (with-open [conn (up/get-conn)]
       (sc/atomic conn
         (sc/execute conn sqlv))))))

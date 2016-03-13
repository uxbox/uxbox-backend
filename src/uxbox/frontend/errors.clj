;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.errors
  "A errors handling for frontend api."
  (:require [catacumba.core :as ct]
            [catacumba.http :as http]
            [catacumba.serializers :as sz]
            [catacumba.handlers.auth :as cauth]
            [catacumba.handlers.parse :as cparse]
            [catacumba.handlers.misc :as cmisc]
            [uxbox.services.auth :as auth]
            [uxbox.frontend.core :as ufc]
            [uxbox.frontend.auth :as ufa]
            [uxbox.frontend.projects :as ufp])
  (:import java.util.UUID))


(defmulti handler
  (fn [_ err]
    (.printStackTrace err)
    (class err)))

(defmethod handler clojure.lang.ExceptionInfo
  [context err]
  (let [message (.getMessage err)
        data (ex-data err)]
    (-> (ufc/rsp {:message message
                  :payload data})
        (http/bad-request))))

(defmethod handler org.jooq.exception.DataAccessException
  [context err]
  (let [err (.getCause err)
        state (.getSQLState err)
        message (.getMessage err)]
    (case state
      "P0002"
      (-> (ufc/rsp {:message message
                    :code "errors.api.occ"})
          (http/bad-request))

      (-> (ufc/rsp {:message message
                    :code (str "errors.api." state)})
          (http/internal-server-error)))))

(defmethod handler :default
  [context err]
  (println "????" (class err))
  (let [message (.getMessage err)]
    (-> (ufc/rsp {:message message
                  :code "errors.api.unexpected"})
        (http/internal-server-error))))

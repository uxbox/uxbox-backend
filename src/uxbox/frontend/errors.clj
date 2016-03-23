;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.errors
  "A errors handling for frontend api."
  (:require [catacumba.core :as ct]
            [catacumba.http :as http]
            [uxbox.util.response :refer (rsp)]))

(defmulti handler
  (fn [_ err]
    (.printStackTrace err)
    (class err)))

(defmethod handler clojure.lang.ExceptionInfo
  [context err]
  (let [message (.getMessage err)
        response {:code message :payload (ex-data err)}]
    (-> (rsp response)
        (http/bad-request))))

(defmethod handler org.jooq.exception.DataAccessException
  [context err]
  (let [err (.getCause err)
        state (.getSQLState err)
        message (.getMessage err)]
    (case state
      "P0002"
      (-> (rsp {:message message
                :code "errors.api.occ"})
          (http/bad-request))

      (-> (rsp {:message message
                :code (str "errors.api." state)})
          (http/internal-server-error)))))

(defmethod handler :default
  [context err]
  (let [message (.getMessage err)]
    (-> (rsp {:type :error
              :message message
              :code "errors.api.unexpected"})
        (http/internal-server-error))))

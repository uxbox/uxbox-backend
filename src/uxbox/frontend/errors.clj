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

(defmulti handle-exception #(:type (ex-data %)))

(defmethod handle-exception :validation
  [err]
  (let [response (select-keys (ex-data err) [:type :payload])]
    (http/bad-request (rsp response))))

(defmethod handle-exception :auth/wrong-credentials
  [err]
  (let [response (select-keys (ex-data err) [:type :payload])]
    (http/bad-request (rsp response))))

(defmethod handle-exception :default
  [err]
  (.printStackTrace err)
  (let [response (select-keys (ex-data err) [:type :payload])
        response (assoc response :message (.getMessage err))]
    (http/internal-server-error (rsp response))))

(defn handle-data-access-exception
  [err]
  (let [err (.getCause err)
        state (.getSQLState err)
        message (.getMessage err)]
    (case state
      "P0002"
      (-> (rsp {:message message
                :code "errors.api.occ"})
          (http/bad-request))

      (do
        (.printStackTrace err)
        (-> (rsp {:message message
                  :code (str "errors.api." state)})
            (http/internal-server-error))))))

(defn handle-unexpected-exception
  [err]
  (.printStackTrace err)
  (let [message (.getMessage err)]
    (-> (rsp {:message message
              :code "errors.api.unexpected"})
        (http/internal-server-error))))


;; --- Entry Point

(defn handler
  [context err]
  (cond
    (instance? clojure.lang.ExceptionInfo err)
    (handle-exception err)

    (instance? org.jooq.exception.DataAccessException err)
    (handle-data-access-exception err)

    :else
    (handle-unexpected-exception err)))

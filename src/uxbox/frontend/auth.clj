;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.auth
  (:require [catacumba.http :as http]
            [promesa.core :as p]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.frontend.core :refer (validate!)]
            [uxbox.util.response :refer (rsp)]))

(def ^:const ^:private +auth-schema+
  {:username [us/required us/string]
   :password [us/required us/string]
   :scope [us/required us/string]})

(defn login
  [{:keys [data] :as context}]
  (p/alet [data (validate! data +auth-schema+)
           data (assoc data :type :auth/login)
           resp (p/await (sv/novelty data))]
    (http/ok (rsp resp))))

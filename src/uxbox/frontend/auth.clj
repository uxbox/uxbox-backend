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
            [uxbox.util.response :refer (rsp)]))

(def validate-form! (partial us/validate! :form/validation))

(def ^:private auth-schema
  {:username [us/required us/string]
   :password [us/required us/string]
   :scope [us/required us/string]})

(defn login
  [{data :data}]
  (let [data (validate-form! data auth-schema)
        message (assoc data :type :auth/login)]
    (->> (sv/novelty message)
         (p/map #(http/ok (rsp %))))))

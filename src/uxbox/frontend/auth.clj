;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.auth
  (:require [catacumba.handlers.postal :as pc]
            [uxbox.schema :as us]
            [uxbox.frontend.core :refer (-handler)]
            [uxbox.services :as sv]
            [promesa.core :as p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +auth-frame-schema+
  {:username [us/required us/string]
   :password [us/required us/string]
   :scope [us/required us/string]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod -handler [:novelty :auth/login]
  [context {:keys [data dest]}]
  (p/alet [data (us/extract! +auth-frame-schema+ data)
           data (assoc data :type dest)
           resp (p/await (sv/novelty data))]
    (pc/frame resp)))

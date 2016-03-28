;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.schema
  (:refer-clojure :exclude [keyword uuid vector boolean map set])
  (:require [struct.core :as st]
            [uxbox.util.exceptions :as ex])
  (:import java.time.Instant))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validators
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (v/defvalidator max-len
;;   "Validate if `v` is at least smaller that `n`."
;;   {:default-message-format "% must be lees than the maximum."}
;;   [v n]
;;   (let [len (count v)]
;;     (>= len v)))

;; (v/defvalidator min-len
;;   "Validate if `v` is at least larger that `n`."
;;   {:default-message-format "% must be greater than the minimum."}
;;   [v n]
;;   (let [len (count v)]
;;     (>= v len)))

(def datetime
  {:message "must be an instant"
   :optional true
   :validate #(instance? Instant %)})

(def positive
  {:message "must be positive"
   :optional true
   :validate pos?})

(def map
  {:message "must be a map"
   :optional true
   :validate map?})

(def set
  {:message "must be a set"
   :optional true
   :validate set?})

(def coll
  {:message "must be a collection"
   :optional true
   :validate coll?})

(def required st/required)
(def number st/number)
(def integer st/integer)
(def boolean st/boolean)
(def string st/string)
(def in-range st/in-range)
(def uuid-like st/uuid-like)
(def uuid st/uuid)
(def integer-like st/integer-like)
(def boolean-like st/boolean-like)
(def email st/email)
(def validate st/validate)

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

;; --- Validators

(def datetime
  {:message "must be an instant"
   :optional true
   :validate #(instance? Instant %)})

(def required
  (assoc st/required :message "errors.form.required"))

(def string
  (assoc st/string :message "errors.form.string"))

(def number
  (assoc st/number :message "errors.form.number"))

(def integer
  (assoc st/integer :message "errors.form.integer"))

(def boolean
  (assoc st/boolean :message "errors.form.bool"))

(def identical-to
  (assoc st/identical-to :message "errors.form.identical-to"))

(def in-range st/in-range)
(def uuid-str st/uuid-str)
(def uuid st/uuid)
(def integer-str st/integer-str)
(def boolean-str st/boolean-str)
(def email st/email)
(def set st/set)
(def map st/map)
(def coll st/coll)
(def positive st/positive)

(def max-len
  {:message "errors.form.max-len"
   :optional true
   :validate (fn [v n]
               (let [len (count v)]
                 (>= len v)))})

(def min-len
  {:message "errors.form.min-len"
   :optional true
   :validate (fn [v n]
               (>= (count v) n))})

(def uploaded-file
  {:message "errors.form.file"
   :optional true
   :validate #(instance? ratpack.form.UploadedFile %)})

(def path
  {:message "errors.form.path"
   :optional true
   :validate #(instance? java.nio.file.Path %)})

;; --- Public Api

(def validate st/validate)

(defn validate!
  ([data schema]
   (validate! :validation data schema))
  ([type data schema]
   (let [[errors data] (st/validate data schema)]
     (if errors
       (throw (ex/ex-info type errors))
       data))))

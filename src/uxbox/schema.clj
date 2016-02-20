(ns uxbox.schema
  (:refer-clojure :exclude [keyword uuid vector boolean])
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [cuerdas.core :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validators
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(v/defvalidator keyword
  "Validates maybe-an-int is a valid integer.
  For use with validation functions such as `validate` or `valid?`"
  {:default-message-format "%s must be a keyword"}
  [v]
  (keyword? v))

(v/defvalidator uuid
  "Validates maybe-an-int is a valid integer.
  For use with validation functions such as `validate` or `valid?`"
  {:default-message-format "%s must be a uuid instance"}
  [v]
  (instance? java.util.UUID v))

(v/defvalidator vector
  "Validats if `v` is vector."
  {:default-message-format "%s must be a vector instance."}
  [v]
  (vector? v))

(v/defvalidator function
  "Validats if `v` is function."
  {:default-message-format "%s must be a function."}
  [v]
  (fn? v))

(def required v/required)
(def number v/number)
(def integer v/integer)
(def boolean v/boolean)
(def string v/string)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn validate
  ([schema] #(validate schema %))
  ([schema data] (first (b/validate data schema))))

(defn validate!
  ([schema] #(validate! schema %))
  ([schema data]
   (when-let [errors (validate schema data)]
     (let [data {:type :validation
                 :payload errors}]
       (throw (ex-info "Schema validations error." data))))))

(defn valid?
  [validator data]
  (let [result (validator data)]
    (if result
      result
      (let [message (:default-message-format (meta validator))
            message (str/format message data)]
        (throw (ex-info message {}))))))

(defn extract
  [schema data]
  (let [keycoll (keys schema)]
    (select-keys data keycoll)))

(defn extract!
  "Extract and validate."
  [schema data]
  (validate! schema data)
  (extract schema data))

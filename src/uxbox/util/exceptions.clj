;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.exceptions
  "A helpers for work with exceptions.")

(defn ex-info?
  [exc]
  (instance? clojure.lang.ExceptionInfo exc))

(defn check-by-type
  [type]
  (fn [exc]
    (if (ex-info? exc)
      (let [data (ex-data exc)]
        (= type (:type data)))
      false)))

(defn check-by-code
  [code]
  (fn [exc]
    (if (ex-info? exc)
      (let [data (ex-data exc)]
        (= code (:code data)))
      false)))

(defn error
  [& {:keys [type code] :or {type :unexpected} :as payload}]
  {:pre [(keyword? type) (keyword? code)]}
  (let [payload (assoc payload :type type)]
    (ex-info (pr-str code) payload)))

(defmacro raise
  [& args]
  `(throw (error ~@args)))

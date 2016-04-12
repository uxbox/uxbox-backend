;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.core
  (:require [clojure.walk :as walk]
            [cuerdas.core :as str]
            [struct.core :as st]
            [uxbox.util.exceptions :as ex]))

;; --- Main Api

(defmulti -novelty
  (fn [conn data] (:type data)))

(defmulti -query
  (fn [conn data] (:type data)))

(defmethod -novelty :default
  [conn data]
  (throw (ex/ex-info :not-implemented data)))

(defmethod -query :default
  [conn data]
  (throw (ex/ex-info :not-implemented data)))

;; --- Common Helpers

(defn normalize-attrs
  "Recursively transforms all map keys from strings to keywords."
  [m]
  (letfn [(tf [[k v]]
            (let [ks (-> (name k)
                         (str/replace "_" "-"))]
              [(keyword ks) v]))
          (walker [x]
            (if (map? x)
              (into {} (map tf) x)
              x))]
    (walk/postwalk walker m)))

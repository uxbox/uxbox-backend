;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.svgparse
  (:require [clojure.spec :as s]
            [cuerdas.core :as str]
            [suricatta.core :as sc]
            [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [uxbox.config :as cfg]
            [uxbox.util.spec :as us]
            [uxbox.db :as db]
            [uxbox.services.core :as core]
            [uxbox.util.exceptions :as ex])
  (:import org.jsoup.Jsoup))

(defn- parse-double
  [data]
  {:pre [(string? data)]}
  (Double/parseDouble data))

(defn- parse-viewbox
  [data]
  {:pre [(string? data)]}
  (mapv parse-double (str/split data #"\s+")))

(defn- assoc-attr
  [acc attr]
  (let [key (.getKey attr)
        val (.getValue attr)]
    (case key
      "width" (assoc acc :width (parse-double val))
      "height" (assoc acc :height (parse-double val))
      "viewbox" (assoc acc :view-box (parse-viewbox val))
      "sodipodi:docname" (assoc acc :name val)
      acc)))

(defn- parse-attrs
  [element]
  (let [attrs (.attributes element)]
    (reduce assoc-attr {} attrs)))

(defn- parse-svg
  [element]
  (let [innerxml (.html element)
        attrs (parse-attrs element)]
    (merge {:content innerxml} attrs)))

(defn parse
  [data]
  {:pre [(string? data)]}
  (let [document (Jsoup/parse data)
        body-element (.body document)
        svg-element (first (.getElementsByTag body-element "svg"))]
    (when svg-element
      (parse-svg svg-element))))



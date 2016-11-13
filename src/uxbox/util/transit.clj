;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.transit
  (:require [cognitect.transit :as t]
            [uxbox.util.time :as dt])
  (:import java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream))

;; --- Point Handling

(defrecord Point [x y])

(def point-write-handler
  (t/write-handler
   (constantly "point")
   #(into {} %)))

(def point-read-handler
  (t/read-handler map->Point))

;; --- Handlers

(def ^:private +reader-handlers+
  (merge dt/+read-handlers+
         {"point" point-read-handler}))

(def ^:private +write-handlers+
  (merge dt/+write-handlers+
         {Point point-write-handler}))

;; --- Public Api

(defn reader
  ([istream]
   (reader istream nil))
  ([istream {:keys [type] :or {type :json}}]
   (t/reader istream type {:handlers +reader-handlers+})))

(defn read!
  "Read value from streamed transit reader."
  [reader]
  (t/read reader))

(defn writer
  ([ostream]
   (writer ostream nil))
  ([ostream {:keys [type] :or {type :json}}]
   (t/writer ostream type {:handlers +write-handlers+})))

(defn write!
  [writer data]
  (t/write writer data))

(defn decode
  ([data]
   (decode data nil))
  ([data opts]
   (with-open [input (ByteArrayInputStream. data)]
     (read! (reader input opts)))))

(defn encode
  ([data]
   (encode data nil))
  ([data opts]
   (with-open [out (ByteArrayOutputStream.)]
     (let [w (writer out opts)]
       (write! w data)
       (.toByteArray out)))))

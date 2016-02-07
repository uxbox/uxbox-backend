;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns msgbus.serializers
  "A serializers abstraction layer."
  (:require [cognitect.transit :as transit])
  (:import java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti encode
  "Encode data."
  (fn [data type] type))

(defmulti decode
  "Decode data."
  (fn [data type] type))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod encode :transit+json
  [data _]
  (with-open [out (ByteArrayOutputStream.)]
    (let [w (transit/writer out :json)]
      (transit/write w data)
      (.toByteArray out))))

(defmethod encode :transit+msgpack
  [data _]
  (with-open [out (ByteArrayOutputStream.)]
    (let [w (transit/writer out :msgpack)]
      (transit/write w data)
      (.toByteArray out))))

(defmethod decode :transit+json
  [data _]
  (with-open [input (ByteArrayInputStream. data)]
    (let [reader (transit/reader input :json)]
      (transit/read reader))))

(defmethod decode :transit+msgpack
  [data _]
  (with-open [input (ByteArrayInputStream. data)]
    (let [reader (transit/reader input :msgpack)]
      (transit/read reader))))

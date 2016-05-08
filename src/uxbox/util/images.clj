;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.images
  "Images transformation utils."
  (:require [clojure.java.io :as io])
  (:import org.im4java.core.IMOperation
           org.im4java.core.ConvertCmd
           org.im4java.process.Pipe
           java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream))

(defn thumbnail
  ([input] (thumbnail input nil))
  ([input {:keys [size quality format]
           :or {format "jpg"
                quality 92
                size [200 200]}
           :as opts}]
   {:pre [(vector? size)]}
   (with-open [out (ByteArrayOutputStream.)]
     (let [[width height] size
           in (io/input-stream input)
           pipe-in (Pipe. in nil)
           pipe-out (Pipe. nil out)
           op (doto (IMOperation.)
                (.addImage (into-array String ["-"]))
                (.thumbnail (int width) (int height))
                (.gravity "center")
                (.extent (int width) (int height))
                (.quality (double quality))
                (.addImage (into-array String [(str format ":-")])))
           cmd (doto (ConvertCmd.)
                 (.setInputProvider pipe-in)
                 (.setOutputConsumer pipe-out))]
       (.run cmd op (into-array Object []))
       (ByteArrayInputStream. (.toByteArray out))))))

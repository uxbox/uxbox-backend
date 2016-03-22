;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.transit
  (:require [catacumba.serializers :as sz]
            [uxbox.util.datetime :as dt]))

(defn decode
  [data]
  (sz/decode data :transit+json {:handlers dt/+read-handlers+}))

(defn encode
  [data]
  (sz/encode data :transit+json {:handlers dt/+write-handlers+}))

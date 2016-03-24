;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.querystring
  (:require [uxbox.util.transit :as t]
            [buddy.core.codecs :as codecs]))

(defn parse-params
  ([params] (parse-params params nil))
  ([params {:keys [key] :or {key :params} :as opts}]
   (if (contains? params key)
     (-> (get params key)
         (codecs/base64->bytes)
         (t/decode))
     {})))

;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.querystring
  (:require [uxbox.util.transit :as t]))

(defn parse-params
  ([params] (parse-params params nil))
  ([params {:keys [key] :as opts}]
   (if (contains? params key)
     (t/decode (get params key))
     {})))

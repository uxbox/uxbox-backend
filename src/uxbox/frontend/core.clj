;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.core
  (:require [catacumba.impl.handlers :as cih]
            [uxbox.util.transit :as t])
  (:import ratpack.handling.Context
           ratpack.http.Response
           ratpack.http.MutableHeaders))


(deftype Rsp [data]
  cih/ISend
  (-send [_ ctx]
    (let [^Response response (.getResponse ^Context ctx)
          ^MutableHeaders headers (.getHeaders response)]
      (.set headers "content-type" "application/transit+json")
      (cih/-send (t/encode data) ctx))))

(defn rsp
  "A shortcut for create a response instance."
  [data]
  (Rsp. data))

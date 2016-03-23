;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.response
  "A lightweigt reponse type definition.

  At first instance it allows set the appropriate
  content-type headers and encode the body using
  the builtin transit abstraction.

  In future it will allow easy adapt for the content
  negotiation that is coming to catacumba."
  (:require [catacumba.impl.handlers :as ch]
            [uxbox.util.transit :as t])
  (:import ratpack.handling.Context
           ratpack.http.Response
           ratpack.http.MutableHeaders))

(deftype Rsp [data]
  ch/ISend
  (-send [_ ctx]
    (let [^Response response (.getResponse ^Context ctx)
          ^MutableHeaders headers (.getHeaders response)]
      (.set headers "content-type" "application/transit+json")
      (ch/-send (t/encode data) ctx))))

(defn rsp
  "A shortcut for create a response instance."
  [data]
  (Rsp. data))

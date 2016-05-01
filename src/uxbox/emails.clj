;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.emails
  (:require [cuerdas.core :as str]
            [instaparse.core :as insta]
            [clojure.java.io :as io]))

(def ^:private grammar
  (str "message = part*"
       "part = begin header body end; "
       "header = tag* eol; "
       "tag = space keyword; "
       "body = line*; "
       "begin = #'--\\s+begin\\s+'; "
       "end = #'--\\s+end\\s*' eol*; "
       "separator = '--'; "
       "keyword  = #':\\w+'; "
       "space = #'\\s*'; "
       "line = #'.*\\n'; "
       "octet = #'.';  "
       "eol = ('\\n' | '\\r\\n'); "))

(def ^:private parser (insta/parser grammar))
(def ^:private path-template "emails/%(lang)s/%(name)s.mustache")

;; (defn parse
;;   []
;;   (let [text (slurp (io/resource "emails/en/register.mustache"))]
;;     (parser text :output-format :hiccup)))

;; (defn register-email
;;   [& args]
;;   (let [opts (apply hash-map args)]




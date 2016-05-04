;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.emails
  "Email parsing helpers."
  (:require [cuerdas.core :as str]
            [instaparse.core :as insta]
            [clojure.java.io :as io]
            [uxbox.util.template :as tmpl]))

(def ^:private grammar
  (str "message = part*"
       "part = begin header body end; "
       "header = tag eol; "
       "tag = space keyword; "
       "body = line*; "
       "begin = #'--\\s+begin\\s+'; "
       "end = #'--\\s+end\\s*' eol*; "
       "keyword  = #':[\\w\\-]+'; "
       "space = #'\\s*'; "
       "line = #'.*\\n'; "
       "eol = ('\\n' | '\\r\\n'); "))

(def ^:private parser (insta/parser grammar))
(def ^:private email-path "emails/%(lang)s/%(id)s.mustache")

(defn- parse-email-template
  [data]
  (loop [state {} parts (drop 1 (parser data))]
    (if-let [[_ _ header body] (first parts)]
      (let [type (get-in header [1 2 1])
            type (keyword (str/slice type 1))
            content (apply str (map second (rest body)))]
        (recur (assoc state type content)
               (rest parts)))
      [(str/trim (:subject state) " \n")
       (str/trim (:body-text state) " \n")
       (str/trim (:body-html state) " \n")])))

(defn- parse-email
  [data]
  (let [[subject text html] (parse-email-template data)]
    {:subject subject
     :body [:alternatives
            {:type "text/plain; charset=utf-8"
             :content text}
            {:type "text/html; charset=utf-8"
             :content html}]}))

(defn render
  [id {:keys [lang] :or {lang "en"} :as opts}]
  (let [path (str/format email-path {:id (name id) :lang lang})
        data (tmpl/render path opts)
        email (parse-email data)]
    (->> (select-keys opts [:from :reply-to :to :cc :bcc])
         (merge email))))

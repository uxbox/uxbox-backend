;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.export
  "Support for exporting projects."
  (:refer-clojure :exclude [with-open])
  (:require [clojure.java.io :as io]
            [suricatta.core :as sc]
            [uxbox.db :as db]
            [uxbox.sql :as sql]
            [uxbox.util.closeable :refer (with-open)]
            [uxbox.util.tempfile :as tmpfile]
            [uxbox.util.transit :as t]
            [uxbox.util.snappy :as snappy]))

(defn- write-pages
  [conn writer id]
  (let [sql (sql/get-pages-for-project {:project id})
        results (sc/fetch conn sql)]
    (println "write-pages:" results)
    (run! #(t/write! writer {::type :page ::payload %}) results)))

(defn- write-pages-history
  [conn writer id]
  (let [sql (sql/get-page-history-for-project {:project id})
        results (sc/fetch conn sql)]
    (println "write-pages-history:" results)
    (run! #(t/write! writer {::type :page-history ::payload %}) results)))

(defn- export-project
  [id]
  (let [path (tmpfile/create)]
    (with-open [ostream (io/output-stream path)
                zstream (snappy/output-stream ostream)
                conn (db/connection)]
      (let [writer (t/writer zstream {:type :msgpack})]
        (write-pages conn writer id)
        (write-pages-history conn writer id)
        path))))


(ns uxbox.services
  "Main namespace for access to all uxbox services."
  (:require [suricatta.core :as sc]
            [catacumba.serializers :as sz]
            [catacumba.impl.executor :as exec]
            [clj-uuid :as uuid]
            [uxbox.persistence :as up]
            [uxbox.services.core :as usc]
            [uxbox.services.auth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Impl.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- encode
  [data]
  (-> (sz/encode data :transit+json)
      (sz/bytes->str)))

(defn- insert-txlog
  [conn data]
  (let [sql (str "INSERT INTO txlog (id, payload, created_at) "
                 "VALUES (?, ?, current_timestamp)")
        sqlv [sql (uuid/v4) (encode data)]]
    (sc/execute conn sqlv)))

(defn- handle-novelty
  [data]
  (with-open [conn (sc/context up/datasource)]
    (sc/atomic conn
      (binding [up/*ctx* conn]
        (usc/-novelty data))
      (insert-txlog conn data))))

(defn- handle-query
  [data]
  (with-open [conn (sc/context up/datasource)]
    (sc/atomic conn
      (binding [up/*ctx* conn]
        (usc/-query data)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn novelty
  [data]
  (exec/submit (partial handle-novelty data)))

(defn query
  [data]
  (exec/submit (partial handle-query data)))

(ns uxbox.msgbus.util
  (:import java.util.UUID))

(defn interrumpted?
  []
  (.isInterrupted (Thread/currentThread)))

(defn message-id
  [msg]
  (str (UUID/randomUUID)))


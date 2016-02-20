(ns uxbox.frontend.auth
  (:require [catacumba.handlers.postal :as pc]
            [uxbox.frontend.core :refer (-handler)]
            [promesa.core :as p]))

(defmethod -handler [:query :auth]
  [context in-frame]
  (p/alet [data {:message "hello world"}]
    (pc/frame data)))

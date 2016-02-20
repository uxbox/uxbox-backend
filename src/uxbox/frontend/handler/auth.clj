(ns uxbox.frontend.handler.auth
  (:require [catacumba.handlers.postal :as pc]
            [promesa.core :as p]
            [uxbox.frontend.handler :refer (-handler)]))

(defmethod -handler [:query :auth]
  [context in-frame]
  (pc/frame {:message "hello world"}))

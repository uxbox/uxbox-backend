(ns uxbox.backend.services)

(def ^:const +hierarchy+
  (as-> (make-hierarchy) $
    (derive $ :auth/login :uxbox/command)
    (derive $ :test/test :uxbox/command)))

(defmulti -handler :cmd
  :hierarchy #'+hierarchy+)

(defmethod -handler :test/test
  [request]
  {:message "hello world"})

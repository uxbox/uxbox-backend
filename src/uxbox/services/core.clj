(ns uxbox.services.core)

(def +hierarchy+
  (as-> (make-hierarchy) $
    (derive $ :auth/login :command)))

(defmulti -novelty :type
  :hierarchy #'+hierarchy+)

(defmulti -query :type
  :hierarchy #'+hierarchy+)

(defmethod -novelty :default
  [data]
  (throw (ex-info "Not implemented" {})))

(defmethod -query :default
  [data]
  (throw (ex-info "Not implemented" {})))


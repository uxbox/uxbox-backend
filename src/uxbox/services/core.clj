(ns uxbox.services.core)

(def +hierarchy+
  (as-> (make-hierarchy) $
    (derive $ :auth/login :command)))

(defmulti -novelty
  (fn [conn data] (:type data))
  :hierarchy #'+hierarchy+)

(defmulti -query
  (fn [conn data] (:type data))
  :hierarchy #'+hierarchy+)

(defmethod -novelty :default
  [data]
  (throw (ex-info "Not implemented" {})))

(defmethod -query :default
  [data]
  (throw (ex-info "Not implemented" {})))


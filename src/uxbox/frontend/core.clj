(ns uxbox.frontend.core)

(defmulti -handler
  (comp (juxt :type :dest) second vector))

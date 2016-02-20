(ns uxbox.frontend.handler)

(defmulti -handler
  (comp (juxt :type :dest) second vector))

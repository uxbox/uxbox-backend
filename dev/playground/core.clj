(ns playground.core
  "Main ns for playground/experiments."
  (:require [haahka.core :as ha]))

(def +system+ (ha/system "default"))


(defn counter-actor-factory
  [n]
  (println "initializing actor")
  (let [counter (volatile! n)]
    (reify ha/IActor
      (-on-message [_ actor message]
        (println "Received message (" @counter "): " message)
        (vswap! counter inc)))))

(def counter-actor (ha/actor counter-actor-factory))
(def a1 (ha/actor-of +system+ (counter-actor 0) "test"))

(defn -main
  [& args]
  (println "Hello world" +system+))

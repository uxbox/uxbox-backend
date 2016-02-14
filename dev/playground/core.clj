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


(comment
  (defn counter4-actor
    [{:keys [state sender] :as actor} message]
    (println "Received message (" @counter "): " message)
    (ha/tell sender @counter)
    (vswap! state update :counter (fnil inc 0)))


  (defn counter2-actor-factory
    [n]
    (let [counter (volatile! n)]
      (fn [actor message]
        (println "Received message (" @counter "): " message)
        (vswap! counter inc))))

  (defn counter3
    [self n]
    (loop [counter n]
      (let [{:keys [payload sender]} (a/<! self)]
        (println "Received message: (" @counter "): " message)
        (recur (inc counter)))))

  (defn counter3
    {:type :actor/channels}
    [{:keys [sender] :as self} n]
    (go-loop [counter n]
      (let [{:keys [payload sender]} (ha/recv! self)]
        (println "Received message: (" @counter "): " message)

        (
        (a/!> sender [@counter self]
        (recur (inc counter)))))))

(defn -main
  [& args]
  (println "Hello world" +system+))

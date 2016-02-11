(ns haahka.core
  (:import akka.actor.Props
           akka.actor.ActorSystem
           akka.actor.UntypedActor
           akka.actor.ActorRef
           akka.actor.ActorRefFactory
           akka.pattern.Patterns
           akka.japi.Creator
           scala.concurrent.Future
           java.util.concurrent.CompletableFuture
           ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstractions / Protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IActorRef
  (-tell [_ message sender] "Fire-and-forget method.")
  (-ask [_ message timeout] "Req-rep method."))

(defprotocol IActorFactory
  (-actor-of [_ props params]))

(defprotocol IActor
  "Main actor abstraction."
  (-on-message [_ actor message] "Handle message."))

(defprotocol IActorHooks
  (-pre-start [_ actor])
  (-post-stop [_ actor]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn system
  ([] (ActorSystem/create))
  ([name] (ActorSystem/create name)))

(defn actor?
  [v]
  (instance? Props v))

(defmulti actor
  "A actor spec constructor."
  (fn [factory]
    (let [md (meta factory)]
      (:actor/type md))))

(defn actor-of
  ([system props]
   {:pre [(actor? props)]}
   (-actor-of system props nil))
  ([system props name]
   {:pre [(actor? props)]}
   (-actor-of system props (str name))))

(defn tell!
  "Send a message to an actor."
  ([ref message]
   (-tell ref message nil))
  ([ref message sender]
   (-tell ref message sender)))

(defn ask!
  ([ref message]
   (-ask ref message 0))
  ([ref message timeout]
   (-ask ref message timeout)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defmethod actor :default
  [spec-factory]
  (fn [& args]
    (Props/create UntypedActor
                  (proxy [Creator] []
                    (create []
                      (let [spec (apply spec-factory args)]
                        (proxy [UntypedActor] []
                          (onReceive [message]
                            (-on-message spec this message))
                          (preStart []
                            (if (satisfies? IActorHooks spec)
                              (-pre-start spec this)
                              (proxy-super preStart)))
                          (postStop []
                            (if (satisfies? IActorHooks spec)
                              (-post-stop spec this)
                              (proxy-super postStop))))))))))

(extend-protocol IActorFactory
  ActorRefFactory
  (-actor-of [system spec name]
    (if name
      (.actorOf system spec name)
      (.actorOf system spec)))

  UntypedActor
  (-actor-of [actor spec name]
    (let [context (.getContext actor)]
      (-actor-of context spec name))))

(defn- future->completable-future
  [^scala.concurrent.Future sf]
  (let [rs (CompletableFuture.)]
    (.onComplete sf (proxy [akka.dispatch.OnComplete] []
                      (onComplete [failure success]
                        (if failure
                          (.completeExceptionally rs failure)
                          (.complete rs success)))))
    rs))

(extend-protocol IActorRef
  ActorRef
  (-tell [ref message sender]
    (.tell ref message sender))
  (-ask [ref message timeout]
    (-> (Patterns/ask ref message timeout)
        (future->completable-future))))

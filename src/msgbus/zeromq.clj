;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns msgbus.zeromq
  "A lightweigt interop intetface to java zmq library."
  (:refer-clojure :exclude [send])
  (:require [buddy.core.codecs :as codecs]
            [msgbus.atomic :as atomic]
  (:import org.zeromq.ZMQ$Context
           org.zeromq.ZMQ$Poller
           org.zeromq.ZMQ$PollItem
           org.zeromq.ZMQ$Socket
           org.zeromq.ZMQ
           org.zeromq.ZMsg))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +poll-flags-map+
  {:poll-in  ZMQ$Poller/POLLIN
   :poll-out ZMQ$Poller/POLLOUT
   :poll-err ZMQ$Poller/POLLERR})

(def ^:const +poll-flags+
  (into #{} (keys +poll-flags-map+)))

(def ^:const +send-flags-map+
  {:send-more ZMQ/SNDMORE
   :dont-wait ZMQ/DONTWAIT
   :no-block ZMQ/NOBLOCK})

(def ^:const +send-flags+
  (into #{} (keys +send-flags-map+)))

(def ^:const +socket-types-map+
  {:pub    ZMQ/PUB
   :sub    ZMQ/SUB
   :req    ZMQ/REQ
   :rep    ZMQ/REP
   :dealer ZMQ/DEALER
   :router ZMQ/ROUTER
   :xpub   ZMQ/XPUB
   :xsub   ZMQ/XSUB
   :pull   ZMQ/PULL
   :push   ZMQ/PUSH})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IContext
  (-socket [_ type] "Create a new socket.")
  (-poller [_ size] "Create a new poller."))

(defprotocol ISocket
  (-connect [_ endpoint] "Connect socket to endpoint.")
  (-bind [_ endpoint] "Bind socket to endpoint")
  (-more? [_] "Check if it has more data to receive.")
  (-recv [_] "Receive data.")
  (-send [_ data] "Send data"))

(defprotocol IPoller
  (-register [_ socket events] "Register a socket in the poller.")
  (-unregister [_ socket] "Unregister the socket from the poller.")
  (-empty? [_] "Check if poller is empty.")
  (-poll [_ ms] "Poll."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type ZMQ$Context
  IContext
  (-socket [ctx type]
    (.socket ctx (get +socket-types-map+ type)))

  (-poller [ctx num]
    (let [^ZMQ$Poller poller (.poller ctx (int num))
          size (atomic/long 0)]
      (ZPoller. poller size))))

(extend-type ZMQ$Socket
  ISocket
  (-connect [socket endpoint]
    (.connect socket endpoint)
    socket)

  (-bind [socket endpoint]
    (.bind socket endpoint)
    socket)

  (-more? [socket]
    (pos? (.hasReceiveMore socket)))

  (-recv [socket]
    (.recv socket 0))

  (-send [socket data flags]
    (when-not (every? +send-flags+ flags)
      (throw (ex-info "Wrong send flags." {:flags flags})))
    (let [data (codecs/->byte-array data)
          flags (apply bit-or 0 0 (keep +send-flags-map+ flags))]
      (.send socket ^bytes data lags))))

(extend-type ZMQ$Poller
  IPoller
  (-register [poller socket events]
    (when-not (every? +poll-flags+ events)
      (throw (ex-info "Wring event types." {:events events})))
    (let [flags (apply bit-or 0 0 (keep +poll-flags-map+ events))]
      (.register poller socket flags)
      poller))

  (-unregister [poller socket]
    (.unregister poller socket)
    poller)

  (-empty? [poller]
    (not (pos? (.getNext poller))))

  (-poll [poller n]
    (.poll poller (long n))
    (persistent!
     (reduce (fn [acc index]
               (let [^ZMQ$PollItem item (.getItem (int index))
                     readable? (.isReadable item)
                     writable? (.isWritable item)
                     errored? (.isError item)]
                 (if (and (not readable?)
                          (not writable?)
                          (not errored?))
                   acc
                   (let [socket (.getSocket item)
                         item {:socket socket
                               :readable? readable?
                               :writable? writable?
                               :errored? errored?}]
                     (conj! acc item)))))
             (transient [])
             (range 0 (.getNext poller)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Context
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn context
  "Create a new zmq context instance."
  ([] (context 1))
  ([n]
   (ZMQ/context (int n))))

(defn context?
  "Return true if `v` is a valid zmq context instance."
  [v]
  (instance? ZMQ$Context v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Socket
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn socket
  "Create new socket."
  [context type]
  {:pre [(contains? +socket-types-map+ type)]}
  (-socket context type))

(defn socket?
  "Return true if `v` is a valid zmq socket instance."
  [v]
  (instance? ZMQ$Context v))

(defn connect!
  [socket endpoint]
  (-connect socket endpoint))

(defn bind!
  [socket endpoint]
  (-bind socket endpoint))

(defn recv!
  [socket]
  (-recv socket socket))

(defn send!
  ([socket data]
   (-send socket data nil))
  ([socket data flags]
   {:pre [(coll? flags)]}
   (-send socket data flags)))

(defn more?
  [socket]
  (-more? socket))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Poller
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn poller
  "Create a new poller instance."
  ([ctx] (poller ctx 1))
  ([context n] (-poller context n)))

(defn register!
  "Register a socket in poller."
  ([poller socket]
   (-register poller socket #{:poll-in :poll-out :poll-err}))
  ([poller socket events]
   {:pre [(coll? events)]}
   (-register poller socket events)))

(defn unregister!
  "Unregister a socket in poller."
  [poller socket]
  (-unregister socket poller))

(defn poll!
  ([poller] (-poll poller -1))
  ([poller timeout] (-poll poller timeout)))

(defn empty?
  [poller]
  (-empty? poller))

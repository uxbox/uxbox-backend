;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns uxbox.msgbus.zmq
  "A lightweigt interop intetface to java zmq library."
  (:refer-clojure :exclude [send])
  (:require [buddy.core.codecs :as codecs])
  (:import org.zeromq.ZMQ$Context
           org.zeromq.ZMQ$Poller
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
  (-poll [_ ms] "Poll."))

(deftype ZContext [^ZMQ$Context ctx]
  java.lang.AutoCloseable
  (close [_] (.destroy ctx)))

(deftype ZSoket [^ZContext context ^ZMQ$Socket socket connections bindings]
  java.lang.AutoCloseable
  (close [_] (.destroy socket)))

(deftype ZPoller [^ZContext context ^ZMQ$Poller poller sockets])
(defrecord ZPollItem [^ZSoket socket readable? writable? errored?])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type ZContext
  IContext
  (-socket [it type]
    (let [^ZMQ$Context ctx (.-ctx it)
          ^ZMQ$Socket sock (.socket ctx (get +socket-types-map+ type))
          connections (atom #{})
          bindings (atom #{})]
      (ZSocket. it sock connections bindings)))

  (-poller [it num]
    (let [^ZMQ$Context ctx (.-ctx it)
          ^ZMQ$Poller poller (.poller ctx (int num))
          sockets (atom [])]
      (ZPoller. it poller sockets))))

(extend-type ZSocket
  ISocket
  (-connect [it endpoint]
    (let [^ZMQ$Socket socket (.-socket it)
          connections (.-connections it)]
      (.connect socket endpoint)
      (swap! connections conj endpoint)
      it))

  (-bind [it endpoint]
    (let [^ZMQ$Socket socket (.-socket it)
          bindings (.-bindings it)]
      (.bind socket endpoint)
      (swap! bindings conj endpoint)
      it))

  (-more? [it]
    (let [^ZMQ$Socket socket (.-socket it)]
      (pos? (.hasReceiveMore socket))))

  (-recv [it]
    (let [^ZMQ$Socket socket (.-socket it)]
      (.recv socket 0)))

  (-send [it data flags]
    (when-not (every? +send-flags+ flags)
      (throw (ex-info "Wrong send flags." {:flags flags})))
    (let [^ZMQ$Socket socket (.-socket it)
          data (codecs/->byte-array data)
          flags (apply bit-or 0 0 (keep +send-flags-map+ flags))]
      (.send socket ^bytes data lags))))

(extend-type ZPoller
  IPoller
  (-register [it zsocket events]
    (when-not (every? +poll-flags+ events)
      (throw (ex-info "Wring event types." {:events events})))

    (let [^ZMQ$Poller poller (.-poller it)
          ^ZMQ$Socket socket (.-socket zsocket)
          sockets (.-sockets it)
          flags (apply bit-or 0 0 (keep +poll-flags-map+ events))
          index (.register poller socket flags)]
      (swap! sockets conj [index zsocket events])
      it))

  (-unregister [it zsocket]
    (let [^ZMQ$Poller poller (.-poller it)
          ^ZMQ$Socket socket (.-socket zsocket)
          sockets (.-sockets it)]
      (.unregister poller socket)
      (swap! sockets (fn [coll]
                       (remove #(= (first %) zsocket) coll)))
      it))

  (-poll [it n]
    (let [^ZMQ$Poller poller (.-poller it)
          sockets @(.-sockets it)]
      (.poll poller (long n))
      (persistent!
       (reduce (fn [acc [index zsocket events]]
                 (let [readable? (pos? (.pollin poller index))
                       writable? (pos? (.pollout poller index))
                       error? (pos? (.pollerr poller index))]
                   (conj! acc (ZPollItem. zsocket readable? writable? error?))))
               (transient [])
               sockets))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Context
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn context
  "Create a new zmq context instance."
  ([] (context 1))
  ([n]
   (ZContext. (ZMQ/context (int n)))))

(defn context?
  "Return true if `v` is a valid zmq context instance."
  [v]
  (instance? ZContext v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Socket
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn socket
  "Create new socket."
  [^ZContext context type]
  {:pre [(contains? +socket-types-map+ type)]}
  (-socket context type))

(defn socket?
  "Return true if `v` is a valid zmq socket instance."
  [v]
  (instance? ZSocket v))

(defn connect!
  [^ZSocket socket ^String endpoint]
  (-connect socket endpoint))

(defn bind!
  [^ZSocket socket ^String endpoint]
  (-bind socket endpoint))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Socket
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


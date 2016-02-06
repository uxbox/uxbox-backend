(ns uxbox.util.zmq
  "A lightweigt interop intetface to java zmq library."
  (:refer-clojure :exclude [send])
  (:require [buddy.core.codecs :as codecs])
  (:import org.zeromq.ZMQ$Context
           org.zeromq.ZMQ$Poller
           org.zeromq.ZMQ$Socket
           org.zeromq.ZMQ
           org.zeromq.ZMsg))

(def ^:const +poll-in+ ZMQ$Poller/POLLIN)
(def ^:const +poll-out+ ZMQ$Poller/POLLOUT)
(def ^:const +snd-more+ ZMQ/SNDMORE)

(def ^:const +socket-types+
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

(defn context
  "Create a new zmq context instance."
  ([] (ZMQ/context 1))
  ([n] (ZMQ/context (int n))))

(defn context?
  "Return true if `v` is a valid zmq context instance."
  [v]
  (instance? ZMQ$Context v))

(defn socket
  "Create new socket."
  [^ZMQ$Context ctx type]
  {:pre [(context? ctx)
         (contains? +socket-types+ type)]}
  (.socket ctx (get +socket-types+ type)))

(defn socket?
  "Return true if `v` is a valid zmq socket instance."
  [v]
  (instance? ZMQ$Socket v))

(defn connect
  [^ZMQ$Socket socket ^String endpoint]
  (.connect socket endpoint)
  socket)

(defn bind
  [^ZMQ$Socket socket ^String endpoint]
  (.bind socket endpoint)
  socket)

(defn poller
  "Create a new poller instance."
  ([ctx] (poller ctx 1))
  ([^ZMQ$Context ctx n]
   (.poller ctx (int n))))

(defn register!
  "Register a socket in poller."
  ([^ZMQ$Poller poller ^ZMQ$Socket socket]
   (.register poller socket))
  ([^ZMQ$Poller poller ^ZMQ$Socket socket events]
   (.register poller socket (int apply bit-or 0 events))))

(defn unregister!
  "Unregister a socket in poller."
  [^ZMQ$Poller poller ^ZMQ$Socket socket]
  (.unregister poller socket))

(defn poll!
  ([^ZMQ$Poller poller]
   (.poll poller))
  ([^ZMQ$Poller poller ^long timeout]
   (.poll poller timeout)))

(defn readable?
  [^ZMQ$Poller poller index]
  (.pollin poller (int index)))

(defn writable?
  [^ZMQ$Poller poller index]
  (.pollout poller (int index)))

(defn recv!
  [^ZMQ$Socket socket]
  {:pre [(socket? socket)]}
  (.recv socket 0))

(defn send!
  ([^ZMQ$Socket socket data] (send socket data 0))
  ([^ZMQ$Socket socket data flags]
   (let [^bytes data (codecs/->byte-array data)
         flags (if (coll? flags)
                 (apply bit-or 0 flags)
                 flags)]
     (.send socket data flags))))

(defn more?
  [^ZMQ$Socket socket]
  {:pre [(socket? socket)]}
  (pos? (.hasReceiveMore socket)))


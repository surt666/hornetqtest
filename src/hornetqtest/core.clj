(ns hornetqtest.core
  (:import (org.hornetq.api.core.client HornetQClient MessageHandler)
           (org.hornetq.api.core UDPBroadcastGroupConfiguration TransportConfiguration DiscoveryGroupConfiguration)
           (org.hornetq.core.remoting.impl.netty NettyConnectorFactory)))

(def group-address "231.7.7.7")

(def group-port 9876)

(def refresh-timeout 10000)

(def discovery-initial-wait-timeout 10000)

(def pico5 "10.146.68.45")

(def pico6 "10.146.68.46")

(def server1 {"host" pico5 "port" 5445})

(def server2 {"host" pico6 "port" 5445})

(defn set-connection-retry-params [locator]
  (. locator (setRetryInterval 2000))
  (. locator (setRetryIntervalMultiplier 2.0))
  (. locator (setMaxRetryInterval 20000))
  (. locator (setConfirmationWindowSize 200000))
  (. locator (setClientFailureCheckPeriod 3000))
  (. locator (setFailoverOnInitialConnection true))
  (. locator (setInitialConnectAttempts -1)))

;;TODO figure out exactly how windowsize corresponds to ackbatchsize. Maybe ackbatchsize should just be 0
(defn create-session [username passwd auto-commit-sends auto-commit-acks pre-ack batch-size]
  (let [locator (HornetQClient/createServerLocatorWithHA (DiscoveryGroupConfiguration. refresh-timeout discovery-initial-wait-timeout (UDPBroadcastGroupConfiguration. group-address group-port nil -1)))
        _ (set-connection-retry-params locator)
        factory (.createSessionFactory locator)]
    (. factory (createSession username passwd false auto-commit-sends auto-commit-acks pre-ack batch-size))))

(defn create-static-session [servers username passwd auto-commit-sends auto-commit-acks pre-ack batch-size]
  (let [configs (map #(TransportConfiguration. (.getName (.getClass (NettyConnectorFactory.))) %) servers)
        locator (HornetQClient/createServerLocatorWithHA (into-array TransportConfiguration configs))
        _ (set-connection-retry-params locator)
        factory (.createSessionFactory locator)]
    (. factory (createSession username passwd false auto-commit-sends auto-commit-acks pre-ack batch-size))))

(defn default-session []
  (create-session "kasia2" "kasia2" true true false 1))

(defn default-static-session []
  (create-static-session [server1 server2] "kasia2" "kasia2" true true false 1))

(defn create-queue [session address queue durable]
  (. session (createQueue address queue durable)))

(defn create-temp-queue [session address queue]
  (. session (createTemporaryQueue address queue)))

(defn create-message [session message durable]
  (let [msg (. session (createMessage durable))]
    (. (.getBodyBuffer msg) (writeString message))
    msg))

(deftype Handler [f]
  MessageHandler
  (onMessage [_ msg]
    (f msg)))

(defn message-handler [f] (Handler. f))

(defn get-producer [session queue]
  (. session (createProducer queue)))

(defn get-consumer [session queue]
  (. session (createConsumer queue)))

(defn start-async-consumer [session queue handler]
  (let [c (. session (createConsumer queue))]
    (. c (setMessageHandler (message-handler handler))) ;;for client acknowledge call acknowledge on message
    c))

(defn send-msg [p msg]
  (. p (send msg)))

(defn send-msg-addr [p msg addr]
  (. p (send addr msg)))

(defn receive-msg [c]
  (let [m (.receive c)]
    (.readString (.getBodyBuffer m))))

(defn receive-msg-immediate [c]
  (let [m (.receiveImmediate c)]
    (prn "M" (.readString (.getBodyBuffer m)))))

(defn send-msg-on-queue [msg queue durable]
  (let [s (default-static-session)
        p (get-producer s queue)
        m (create-message s msg durable)]
    (.start s)
    (send-msg p m)
    (.stop s)
    (.close s)))

(defn receive-session [qn]
  (let [s (default-static-session)
        q (create-temp-queue s qn qn)
        p (get-producer s qn)
        m (create-message s "Hej" false)]
    (send-msg p m)
    (.start s)
    (let [c (get-consumer s qn)]
      (prn (receive-msg c)))
    (.close s)))

(defn setup-async [qn]
  (let [s (default-static-session)
        q (create-queue s qn qn true)
       ; p (get-producer s qn)
       ; m (create-message s "HEJ" true)
        ]
    (start-async-consumer s qn (fn [msg] (prn "MSG = " (.readString (.getBodyBuffer msg)))))
    (.start s)
    [s]))

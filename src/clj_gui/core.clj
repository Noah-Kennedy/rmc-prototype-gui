(ns clj-gui.core
  (:require pyro.printer
            [fn-fx.diff :refer [component defui render should-update?]]
            [manifold.stream :as s]
            [clojure.string :as string]
            [clj-gui.tcp :refer [client!]]
            [clj-gui.gui :refer [stage]]
            [fn-fx.fx-dom :as dom])
  (:import (java.util.regex Pattern)))

(set! *warn-on-reflection* true)
(pyro.printer/swap-stacktrace-engine!)

(def gui-state (ref {:right-actuator "0"
                     :left-actuator  "0"
                     :tcp-status     false
                     :udp-status     false
                     :tcp-address    "NA"
                     :udp-address    "NA"
                     :tcp-port       "NA"
                     :udp-port       "NA"}))

(def tcp-server (ref nil))

(defn send-to-robot! [message]
  (s/put! (:stream @tcp-server) message))

(defn lookup-message-handler
  [report]
  (get {"hello" (fn [_]
                  (dosync
                    (alter gui-state assoc :tcp-status true)
                    (alter gui-state assoc
                           :tcp-address (:address (ensure tcp-server)))
                    (alter gui-state assoc
                           :tcp-port (:port (ensure tcp-server)))))
        "test"  (fn [_] (println "test"))}
       report))

(defn handle-new-message
  "Handles a new incoming message that was sent by the GUI."
  [message]
  (let [tokens (string/split message (Pattern/compile " "))
        command (first tokens)
        args (rest tokens)]
    ((lookup-message-handler command) args)))

(defn launch-client! [address port]
  (dosync (when (some? @tcp-server)
            (let [server (:stream (ensure tcp-server))]
              (when (and (some? server)
                         (not (s/closed? server)))
                (s/close! server))))
    (println (str "Connecting to " address " at port " port "..."))
      (let [client-stream @(client! address (Integer/parseInt port))]
        (println "Connected")
        (ref-set tcp-server
                 {:stream  client-stream
                  :address address
                  :port    port})
        (println "Setting up message handler")
        (s/consume-async
          (comp handle-new-message
                (fn [bytes]
                  (byte-streams/convert bytes String)))
          client-stream)
        (println "Done."))))

(defn gui-event-handler [event]
  (case (get event :event)
    :tcp-connect (let [fields (get event :fn-fx/includes)
                       address (-> fields
                                   (get :tcp-address-field)
                                   (get :text))
                       port (-> fields
                                (get :tcp-port-field)
                                (get :text))]
                   (dosync
                     (launch-client! address (str port))
                     (println "Launched")
                     (println (str "Handled: " event))))))

(defn launch-gui! []
  (let [ui-state (dosync (agent (dom/app (stage (ensure gui-state)) gui-event-handler)))]
    (add-watch gui-state
               :ui (fn [_ _ _ _]
                     (send ui-state
                           (fn [old-ui]
                             (dosync (dom/update-app old-ui
                                                     (stage (ensure gui-state))))))))))
(defn -main []
  (do
    (launch-gui!)))
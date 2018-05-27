(ns clj-gui.core
  (:require pyro.printer
            [fn-fx.fx-dom :as dom]
            [fn-fx.diff :refer [component defui render should-update?]]
            [fn-fx.controls :as ui]
            [aleph.tcp :as tcp]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.string :as string]
            [gloss.core :as gloss]
            [gloss.io :as io]
            [clojure.edn :as edn])
  (:import (java.util.regex Pattern)))

(declare stage)
(declare main-window)

(def protocol (gloss/string :utf-8 :delimiters ["\n"]))

(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
      (s/map #(io/encode protocol %) out)
      s)
    (s/splice
      out
      (io/decode-stream s protocol))))

(defn client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream protocol %)))

(pyro.printer/swap-stacktrace-engine!)

(def tcp-server (ref nil))

(defn send-to-robot [message]
  (s/put! (:stream @tcp-server) message))

(def gui-state (ref {:right-actuator "0"
                     :left-actuator  "0"
                     :tcp-status     false
                     :udp-status     false
                     :tcp-address    "NA"
                     :udp-address    "NA"
                     :tcp-port       "NA"
                     :udp-port       "NA"}))

(defui MainWindow
  (render [this {:keys [right-actuator
                        left-actuator
                        tcp-status
                        udp-status
                        tcp-address
                        udp-address
                        tcp-port
                        udp-port]}]
    (ui/border-pane
      :min-width 1500
      :min-height 1000
      :right (ui/grid-pane
               :alignment :top-left
               :hgap 10
               :vgap 10
               :padding (ui/insets
                          :bottom 25
                          :top 25
                          :left 25
                          :right 25)
               :children [(ui/label
                            :text "Right Actuator:"
                            :grid-pane/column-index 0
                            :grid-pane/row-index 0)
                          (ui/label
                            :text "Left Actuator:"
                            :grid-pane/column-index 0
                            :grid-pane/row-index 1)
                          (ui/label
                            :text (str right-actuator)
                            :grid-pane/column-index 1
                            :grid-pane/row-index 0)
                          (ui/label
                            :text (str left-actuator)
                            :grid-pane/column-index 1
                            :grid-pane/row-index 1)])
      :left (ui/grid-pane
              :alignment :top-left
              :hgap 10
              :vgap 10
              :padding (ui/insets
                         :bottom 25
                         :top 25
                         :left 25
                         :right 25)
              :children [(ui/label
                           :text "TCP Connection Status:"
                           :grid-pane/row-index 0
                           :grid-pane/column-index 0)
                         (ui/label
                           :text "UDP Connection Status:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 1)

                         (ui/label
                           :text "TCP Server Address:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 2)
                         (ui/label
                           :text "UDP Server Address:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 4)

                         (ui/label
                           :text "TCP Port:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 3)
                         (ui/label
                           :text "UDP Port:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 5)


                         (ui/label
                           :text "Current TCP Server:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 6)
                         (ui/label
                           :text "Current UDP Server:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 8)

                         (ui/label
                           :text "Current TCP Port:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 7)
                         (ui/label
                           :text "Current UDP Port:"
                           :grid-pane/column-index 0
                           :grid-pane/row-index 9)


                         (ui/label
                           ;TCP indicator
                           :text (if tcp-status
                                   "Connected"
                                   "Not Connected")
                           :grid-pane/row-index 0
                           :grid-pane/column-index 1)
                         (ui/label
                           ;UDP indicator
                           :text (if udp-status
                                   "Connected"
                                   "Not Connected")
                           :grid-pane/row-index 1
                           :grid-pane/column-index 1)

                         (ui/label
                           :text (if tcp-status
                                   tcp-address
                                   "NA")
                           :grid-pane/column-index 1
                           :grid-pane/row-index 6)
                         (ui/label
                           :text (if tcp-status
                                   tcp-port
                                   "NA")
                           :grid-pane/column-index 1
                           :grid-pane/row-index 7)
                         (ui/label
                           :text (if udp-status
                                   udp-address
                                   "NA")
                           :grid-pane/column-index 1
                           :grid-pane/row-index 8)
                         (ui/label
                           :text (if udp-status
                                   udp-port
                                   "NA")
                           :grid-pane/column-index 1
                           :grid-pane/row-index 9)
                         (ui/text-field
                           :id :tcp-address-field
                           :text "10.10.10.1"
                           :grid-pane/column-index 1
                           :grid-pane/row-index 2)
                         (ui/text-field
                           :id :udp-address-field
                           :text "10.10.10.1"
                           :grid-pane/column-index 1
                           :grid-pane/row-index 4)
                         (ui/text-field
                           :id :tcp-port-field
                           :text "2401"
                           :grid-pane/column-index 1
                           :grid-pane/row-index 3)
                         (ui/text-field
                           :id :udp-port-field
                           :text "343"
                           :grid-pane/column-index 1
                           :grid-pane/row-index 5)
                         (ui/button
                           :text "Connect TCP"
                           :on-action {:event         :tcp-connect
                                       :fn-fx/include {:tcp-address-field #{:text}
                                                       :tcp-port-field    #{:text}}}
                           :grid-pane/column-index 0
                           :grid-pane/row-index 10)
                         (ui/button
                           :text "Connect UDP"
                           :on-action {:event         :udp-connect
                                       :fn-fx/include {:udp-address-field #{:text}
                                                       :udp-port-field    #{:text}}}
                           :grid-pane/column-index 1
                           :grid-pane/row-index 10)]))))

(defui Stage
  (render [this args]
    (ui/stage
      :min-width 1500
      :min-height 1000
      :title "RMC Prototype GUI"
      :shown true
      :resizable false
      :scene (ui/scene
               :root (main-window args)))))

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

(defn launch-client [address port]
  (do (println (str "Connecting to " address " : " port))
      (let [client-stream @(client address (Integer/parseInt port))]
        (println "Theoretically connected")
        (ref-set tcp-server
                 {:stream  client-stream
                  :address address
                  :port    port})
        (println "Chaining...")
        (s/consume-async
          (comp handle-new-message
                (fn [bytes]
                  (byte-streams/convert bytes String)))
          client-stream)
        (println "Chained."))))

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
                     (launch-client address (str port))
                     (println "Launched")
                     (println (str "Handled: " event))))))

(defn launch-gui []
  (let [ui-state (dosync (agent (dom/app (stage (ensure gui-state)) gui-event-handler)))]
    (add-watch gui-state
               :ui (fn [_ _ _ _]
                     (send ui-state
                           (fn [old-ui]
                             (dosync (dom/update-app old-ui
                                                     (stage (ensure gui-state))))))))))

(defn -main []
  (do
    (launch-gui)))
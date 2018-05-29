(ns clj-gui.gui
  (:require pyro.printer
            [fn-fx.diff :refer [component defui render should-update?]]
            [fn-fx.controls :as ui]))

(declare stage)
(declare main-window)

(defn get-tab-pane-content [right-actuator
                            left-actuator]
  (ui/tab-pane
    :style "-fx-border-color: black"
    :tabs [(ui/tab
             :text "Sensor Data"
             :content (ui/grid-pane
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
                                     :grid-pane/row-index 1)]))]))

(defn get-command-content [console-txt]
  (ui/v-box
    :style "-fx-border-color: black"
    :alignment :top-left
    :children [(ui/grid-pane
                 :style "-fx-border-color: black"
                 :alignment :top-left
                 :hgap 10
                 :vgap 10
                 :padding (ui/insets
                            :bottom 25
                            :top 25
                            :left 25
                            :right 25)
                 :children [(ui/label
                              :text "Command"
                              :grid-pane/column-index 0
                              :grid-pane/row-index 0)
                            (ui/text-field
                              :id :command-field
                              :grid-pane/column-index 1
                              :grid-pane/row-index 0)
                            (ui/button
                              :text "Send off"
                              :on-action {:event         :send-command
                                          :fn-fx/include {:command-field #{:text}}}
                              :grid-pane/column-index 0
                              :grid-pane/row-index 1)

                            (ui/button
                              :text "hello"
                              :on-action {:event :hello}
                              :grid-pane/column-index 0
                              :grid-pane/row-index 2)
                            (ui/button
                              :text "test"
                              :on-action {:event :test}
                              :grid-pane/column-index 1
                              :grid-pane/row-index 2)])
               (ui/text-area
                 :min-height 730
                 :editable false
                 :style "-fx-border-color: black"
                 :text console-txt)]))

(defn get-connection-content [tcp-status
                              udp-status
                              tcp-address
                              udp-address
                              tcp-port
                              udp-port]
  (ui/grid-pane
    :style "-fx-border-color: black"
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
                 :grid-pane/row-index 10)]))

(defui MainWindow
  (render [this {:keys [right-actuator
                        left-actuator
                        tcp-status
                        udp-status
                        tcp-address
                        udp-address
                        tcp-port
                        udp-port
                        console-txt]}]
    (ui/border-pane
      :min-width 1500
      :min-height 1000
      :right (get-command-content console-txt)
      :left (get-connection-content tcp-status
                                    udp-status
                                    tcp-address
                                    udp-address
                                    tcp-port
                                    udp-port)
      :bottom (get-tab-pane-content right-actuator
                                    left-actuator))))

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
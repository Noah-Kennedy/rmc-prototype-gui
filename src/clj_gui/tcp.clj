(ns clj-gui.tcp
  (:require [gloss.core :as gloss]
            [manifold.stream :as s]
            [gloss.io :as io]
            [aleph.tcp :as tcp]
            [manifold.deferred :as d]))

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

(defn client!
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream protocol %)))
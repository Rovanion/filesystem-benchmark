(ns disk-benchmark.core
  (:require [clojure.java.io              :as io]
            [clojure.math.numeric-tower   :as math]
            [disk-benchmark.arguments     :refer [validate-args]]
            [disk-benchmark.utilities     :refer [time]])
  (:gen-class))

(defn write-file
  "Write some data to a file in the given path."
  [data base-path buffer-size]
  (let [path (str base-path "zeros-" (format "%07d" buffer-size))]
    (with-open [out (io/output-stream path :buffer-size buffer-size)]
      (.write out data))
    (list buffer-size (/ (count data) buffer-size))))

(let [file (byte-array (math/expt 2 30))]
  (for [exponent (range 20 29)]
    (time (write-file file path (math/expt 2 exponent)))))


(defn run-benchmark
  "The meat of the code."
  [{:keys path}]
  (time (write-file (byte-array file-size) path (math/expt 2 14))))


(defn -main
  "The entrypoint of the application."
  [& args]
  (let [{:keys [arguments options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (run-benchmark options)))

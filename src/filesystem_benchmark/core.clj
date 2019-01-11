(ns filesystem-benchmark.core
  (:require [clojure.java.io                :as io]
            [clojure.math.numeric-tower     :as math]
            [clj-mmap.core                  :as mmap]
            [filesystem-benchmark.arguments :refer [validate-args]]
            [filesystem-benchmark.utilities :refer [time]]
            [java.nio.Buffer :as buffer])
  (:gen-class))


(defn write-file-output-stream
  [data base-path]
  (let [path        (str base-path "output-stream")
        buffer-size (math/expt 2 12)]
    (with-open [out (io/output-stream path :buffer-size buffer-size)]
      (.write out data))
    path))


(defn write-file-mmap
  "Write some data to a file in the given path."
  [bytes base-path]
  (let [path (str base-path "mmap")]
    (with-open [mapped-file (mmap/get-mmap path :read-write (count bytes))]
      (mmap/put-bytes mapped-file bytes 0))
    path))

(defn write-file-chunked-mmap
  "Write bytes to nr-chunks number of files in base-path.

  Note though that this function doesn't work properly, it writes
  the first part of the byte-array to all chunks, but it was only
  meant for benchmarking and thus the time to make it work for
  real wasn't taken."
  [bytes base-path nr-chunks]
  (let [chunk-size   (/ (count bytes) nr-chunks)
        future-files (for [i (range nr-chunks)]
                       (future (let [path (str base-path "mmap-chunked-" i)]
                                 (with-open [mapped-file (mmap/get-mmap path :read-write chunk-size)]
                                   (mmap/put-bytes mapped-file bytes 0 chunk-size)))))]
    (do (doall (map deref future-files))
        (str base-path "mmap-chunked-" nr-chunks))))

(defn +throughput
  [time-dict size]
  (assoc time-dict :throughput-MBps (/ (/ size (math/expt 2 20))
                                       (/ (:execution-time-ms time-dict) 1000))))

(defn run-write-type-benchmark
  [folder]
  (for [exponent (range 24 31)]
    (let [size   (math/expt 2 exponent)
          data   (byte-array size)]
      [(str "Size: " size "b, " (/ size (math/expt 2 10)) "kB, " (/ size (math/expt 2 20)) "MB")
       (+throughput (time (write-file-mmap           data folder)) size)
       (+throughput (time (write-file-output-stream  data folder)) size)
       (doall (for [nr-chunks '(2 4 8 16 32 64)]
                (+throughput (time (write-file-chunked-mmap data folder nr-chunks)) size)))])))

(defn run-benchmark
  "The meat of the code."
  [{:keys path}]
  (time (write-file (byte-array (math/expt 2 20)) path)))


(defn -main
  "The entrypoint of the application."
  [& args]
  (let [{:keys [arguments options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (run-benchmark options))))

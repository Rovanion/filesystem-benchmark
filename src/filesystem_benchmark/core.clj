(ns filesystem-benchmark.core
  (:require [clojure.java.io                :as io]
            [clojure.java.shell             :refer [sh]]
            [clojure.math.numeric-tower     :as math]
            [clj-mmap.core                  :as mmap]
            [filesystem-benchmark.arguments :refer [validate-args]]
            [filesystem-benchmark.utilities :refer [time-dict now pps log2 pooled-future in?]])
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

  Note though that this function doesn't work properly, it writes the
  first part of the byte-array to all chunks, but it was only meant
  for benchmarking and thus the time to make it work for real wasn't
  taken."
  [bytes base-path nr-chunks]
  (let [chunk-size   (/ (count bytes) nr-chunks)
        future-files (for [i (range nr-chunks)]
                       (pooled-future
                        (let [path (str base-path "mmap-chunked-" i)]
                          (with-open [mapped-file (mmap/get-mmap path :read-write chunk-size)]
                            (mmap/put-bytes mapped-file bytes 0 chunk-size)))))]
    (do (doall (map deref (doall future-files)))
        (str "mmap-write-chunked-" nr-chunks))))

(defn write-file-copies-mmap
  "Write nr-copies of bytes in base-path."
  [bytes base-path nr-copies]
  (let [future-files (for [i (range nr-copies)]
                       (pooled-future
                        (let [path (str base-path "mmap-copies-" i)]
                          (with-open [mapped-file (mmap/get-mmap path :read-write (count bytes))]
                            (mmap/put-bytes mapped-file bytes 0)))))]
    ;; Dereference all future files, that is wait until all futures have produced a value.
    (do (doall (map deref
                    ;; But make sure to create all futures before we start dereferencing.
                    ;; Else we get chunks of 32 produced by range.
                    (doall future-files)))
        (str "mmap-write-copies-" nr-copies))))

(defn +throughput
  [time-dict size]
  (assoc time-dict :throughput-MBps (/ (/ size (math/expt 2 20))
                                       (/ (:execution-time-ms time-dict) 1000))))

(defn run-write-type-benchmark!
  [folder max-size]
  (for [exponent (range 12 (log2 max-size))]
    (let [size   (math/expt 2 exponent)
          data   (byte-array size)]
      [(str "Size: " size "b, " (/ size (math/expt 2 10)) "kB, " (/ size (math/expt 2 20)) "MB")
       (+throughput (time-dict (write-file-mmap           data folder)) size)
       (+throughput (time-dict (write-file-output-stream  data folder)) size)
       (doall (for [nr-chunks '(2 4 8 16 32 64)]
                (+throughput (time-dict (write-file-chunked-mmap data folder nr-chunks)) size)))])))

(defn run-write-throughput-benchmark!
  "Runs a write throughput benchmark with a default file size of
  32MB and default max copies of 1024."
  ([folder]           (run-write-throughput-benchmark! folder (math/expt 2 25)))
  ([folder file-size] (run-write-throughput-benchmark! folder file-size (math/expt 2 10)))
  ([folder file-size concurrency]
   (let [data (byte-array file-size)]
     (for [nr-concurrent-files (map #(math/expt 2 %) (range 0 (log2 (inc concurrency))))]
       (-> (time-dict (write-file-copies-mmap data folder nr-concurrent-files))
           (+throughput (* file-size nr-concurrent-files))
           (assoc :total-MB (* (/ file-size (math/expt 2 20)) nr-concurrent-files)))))))

(defn read-file-mmap
  [path & [size]]
  (with-open [mmapped-file (mmap/get-mmap path :read-only)]
             (mmap/get-bytes mmapped-file 0 (or size (.size mmapped-file)))))


(defn read-file-copies-mmap
  "This function assumes that there in folder is nr-files to read
  named mmap-copies-N."
  [folder nr-files & [size]]
  (let [future-files (for [i (range nr-files)]
                       (pooled-future
                        (let [path (str folder "mmap-copies-" i)]
                          (read-file-mmap path size)
                          "Dummy retval to not blow the heap")))]
    (do (doall (map deref (doall future-files)))
        (str "read-mmap-" nr-files))))

(defn run-read-throughput-benchmark!
  "Runs a read throughput benchmark with a default file size of 32MB
  and default max copies of 1024."
  ([folder]           (run-read-throughput-benchmark! folder (math/expt 2 25)))
  ([folder file-size] (run-read-throughput-benchmark! folder file-size (math/expt 2 10)))
  ([folder file-size concurrency]
   (for [nr-concurrent-files (map #(math/expt 2 %) (range 0 (log2 (inc concurrency))))]
     (do (sh "sudo" "/sbin/sysctl" "vm.drop_caches=3")
         (-> (time-dict (read-file-copies-mmap folder nr-concurrent-files file-size))
             (+throughput (* file-size nr-concurrent-files))
             (assoc :total-MB (* (/ file-size (math/expt 2 20)) nr-concurrent-files)))))))

(defn -bench-out
  [path file-size-mb concurrency name data]
  (spit (str path "/" name file-size-mb "MB-" concurrency "copies-" (now) ".edn") (pps data)))

(defn run-benchmark
  [{:keys [path file-size concurrency benchmarks]}]
  (let [file-size-mb    (/ file-size (math/expt 2 20))
        out             (partial -bench-out path file-size-mb concurrency)
        data-files-path (str path "/data-files/")]
    (io/make-parents (str data-files-path "dummy"))
    (when (in? benchmarks :write-type)
      (println "Starting write type benchmark.")
      (out "write-type-output-" (run-write-type-benchmark!       data-files-path file-size)))
    (when (in? benchmarks :write-throughput)
      (println "Starting write throughput benchmark.")
      (out "write-throughput-"  (run-write-throughput-benchmark! data-files-path file-size concurrency)))
    (when (in? benchmarks :read-throughput)
      (println "Starting read throughput benchmark.")
      (out "read-throughput-"   (run-read-throughput-benchmark!  data-files-path file-size concurrency)))))


(defn -main
  "The entrypoint of the application."
  [& args]
  (let [{:keys [arguments options exit-message ok?]} (validate-args args)]
    (if exit-message
      (do (println exit-message)
          (System/exit (if ok? 0 1)))
      (run-benchmark options))))


(comment
  (-main "--path" "/mnt/machine-learning-storage/ML1/Maintenance/filesystem-benchmark/se03-dgx09/")
  )

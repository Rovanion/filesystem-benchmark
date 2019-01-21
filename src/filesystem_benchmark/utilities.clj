(ns filesystem-benchmark.utilities)

(defmacro time-dict
  "Evaluates expr and returns a map with the return value and time
  it took to execute expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr
         delta# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
     {:retval            ret#
      :execution-time-ms delta#}))


(defn now
  "Get the current date and time"
  []
  (java.time.LocalDateTime/now))

(defn ppw
  "Pretty print with a large column width."
  [& args]
  (binding [clojure.pprint/*print-right-margin* 150]
    (apply clojure.pprint/pprint args)))

(defn pps
  "Pretty print with a large column width to a string."
  [& args]
  (with-out-str (apply ppw args)))


(defn log2 [n]
  (/ (Math/log n) (Math/log 2)))

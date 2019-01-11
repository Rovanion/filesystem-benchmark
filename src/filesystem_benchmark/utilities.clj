(ns filesystem-benchmark.utilities)

(defmacro time
  "Evaluates expr and returns a map with the return value and time
  it took to execute expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr
         delta# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
     {:retval            ret#
      :execution-time-ms delta#}))

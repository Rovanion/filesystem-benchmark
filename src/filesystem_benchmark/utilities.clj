(ns filesystem-benchmark.utilities
  (:refer-clojure :exclude [future-call]))

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

;;; Reimplement future in order to use a different executor.

(defn binding-conveyor-fn
  {:private true
   :added "1.3"}
  [f]
  (let [frame (clojure.lang.Var/cloneThreadBindingFrame)]
    (fn
      ([]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f))
      ([x]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x))
      ([x y]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x y))
      ([x y z]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x y z))
      ([x y z & args]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (apply f x y z args)))))

(defn ^:private deref-future
  ([^java.util.concurrent.Future fut]
     (.get fut))
  ([^java.util.concurrent.Future fut timeout-ms timeout-val]
     (try (.get fut timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)
          (catch java.util.concurrent.TimeoutException e
            timeout-val))))

(defn future-call
  "Takes a function of no args and yields a future object that will
  invoke the function in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block, unless the variant
  of deref with timeout is used. See also - realized?."
  {:added "1.1"
   :static true}
  [f]
  (let [f (binding-conveyor-fn f)
        fut (.submit clojure.lang.Agent/pooledExecutor ^Callable f)]
    (reify
     clojure.lang.IDeref
     (deref [_] (deref-future fut))
     clojure.lang.IBlockingDeref
     (deref
      [_ timeout-ms timeout-val]
      (deref-future fut timeout-ms timeout-val))
     clojure.lang.IPending
     (isRealized [_] (.isDone fut))
     java.util.concurrent.Future
      (get [_] (.get fut))
      (get [_ timeout unit] (.get fut timeout unit))
      (isCancelled [_] (.isCancelled fut))
      (isDone [_] (.isDone fut))
      (cancel [_ interrupt?] (.cancel fut interrupt?)))))

(defmacro pooled-future
  "Takes a body of expressions and yields a future object that will
  invoke the body in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block, unless the variant of
  deref with timeout is used. See also - realized?."
  {:added "1.1"}
  [& body] `(future-call (^{:once true} fn* [] ~@body)))

(defn tprn
  [retvals]
  (prn retvals)
  retvals)

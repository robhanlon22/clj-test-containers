(ns user
  (:require
   [clojure.spec.alpha :as s]
   [clojure.tools.namespace.repl :as c.t.n.repl]
   [expound.alpha :as expound]
   [kaocha.repl]
   [orchestra.spec.test :as st]))

;; Depending on a user's REPL setup, we may or may not be inside of a binding
;; context. Alter the root binding of s/*explain-out*, and set! it if the value
;; is thread-bound.
(alter-var-root #'s/*explain-out* (constantly expound/printer))

(when (thread-bound? #'s/*explain-out*)
  (set! s/*explain-out* expound/printer))

(defn refresh
  []
  (c.t.n.repl/refresh :after `st/instrument))

(defn refresh-all
  []
  (c.t.n.repl/refresh-all :after `st/instrument))

(defn run-tests
  [& args]
  (refresh)
  (apply kaocha.repl/run args))

(defn run-all-tests
  [& args]
  (refresh)
  (apply kaocha.repl/run-all args))

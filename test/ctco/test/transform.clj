;;----------------------------------------------------------------------
;; File transform.clj
;; Written by Chris Frisz
;; 
;; Created 28 Apr 2012
;; Last modified 17 Aug 2012
;; 
;; Test programs for the full CTCO compiler.
;;----------------------------------------------------------------------

(ns ctco.test.transform
  (:use [clojure.test]
        [clojure.core.match
         :only (match)]
        [clojure.walk
         :only (prewalk-replace)]
        [ctco.core
         :only (ctco)]))

(defn- time-eval 
  "Takes a sequence (assumed to be syntax) and returns a sequence that 
  represents an expression that times the evaluation of the input expression
  and returns the evaluated expression and time to evaluate it in a vector."
  [e]
  `(let [start-time# (. java.lang.System (nanoTime))
         eval-it# ~e 
         end-time# (. java.lang.System (nanoTime))]
     [eval-it# (/ (- end-time# start-time#) 1000.0)]))

(defmacro ctco-test-eval
  "Takes a CTCO expression and tests whether the result of evaluating the 
  unmodified expression and the CTCO-transformed expression is the same. It 
  prints the comparison of the times for evaluating the unmodified versus the
  CTCO-modified code."
  [expr]
  `(let [[old-expr# old-time#] ~(time-eval expr)
         [new-expr# new-time#] ~(time-eval (macroexpand `(ctco ~expr)))]
     (and (is (= old-expr# new-expr#))
          (println "Old time: " old-time# "\nNew time: " new-time#)))) 

(defmacro ctco-test-apply
  "Takes a CTCO expression that evaluates to a function (i.e. 'fn' or 'defn')
  and expands into a function that takes a list of arguments and applies the 
  function (both unmodified and CTCO-modified) to the arguments. The results
  are tested for equivalence. Additionally, the timings for both applications 
  are printed."
  [expr]
  (letfn [(rename [e]
            (match [e]
              [(['defn name fml* body] :seq)] (let [new-name (gensym name)]
                                                (prewalk-replace {name new-name} 
                                                                 e)) 
              :else e))]
    (let [arg* (gensym 'arg*)]
    `(fn [~arg*]
       (let [[old-apply# old-time#]
             ~(time-eval `(apply ~(rename expr) ~arg*))
             [new-apply# new-time#]
             ~(time-eval `(apply ~(macroexpand `(ctco ~(rename expr))) ~arg*))]
         (and (is (= old-apply# new-apply#))
              (do
                (println "Old time: " old-time# "ms")
                (println "New time: " new-time# "ms"))))))))

(deftest id
  "Testing the identity function"
  (println "Identity test")
  (let [id-test (ctco-test-apply (defn id [x] x))
        int-arg* (map list (take 10 (iterate inc 0)))]
    (doseq [a int-arg*] (id-test a))))

(deftest fact
  "Testing tail-recursive factorial"
  (println "Factorial test")
  (let [fact-test (ctco-test-apply
                   (defn fact-acc [n a]
                     (if (zero? n)
                         a
                         (fact-acc (dec n) (* n a)))))
        small-arg* (map #(list % 1) (take 10 (iterate inc 0)))
        big-arg* (map #(list (bigint %) 1)
                      (take-while #(<= % 50) (iterate #(+ 10 %) 10)))]
    (doseq [a small-arg*] (fact-test a))
    (doseq [a big-arg*] (fact-test a))))

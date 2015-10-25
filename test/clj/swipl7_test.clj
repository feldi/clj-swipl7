(ns clj.swipl7-test
  (:require [clojure.test :refer :all]
            [clj.swipl7.protocols :refer :all]
            [clj.swipl7.core :refer :all]))

(deftest jpl-tests
  
  (testing "version"
           (is (= (get-jpl-version-as-string) "7.0.1-alpha")))
  
  (testing "prolog lists"
           (let [pl-list (to-pl [(to-pl "a") (to-pl "b") (to-pl "c")])
                 clj-list (pl-list-to-clj-list pl-list)
                 clj-vec (pl-list-to-vec pl-list)]
             (is (= '("a" "b" "c") clj-list))
             (is (= ["a" "b" "c"] clj-vec))))
  
  (testing "prolog exception"
           (is (thrown? org.jpl7.PrologException (build-q "p(]."))))
  
  )

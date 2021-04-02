(ns 
  ^{:author "Peter Feldtmann"
    :doc "Demo code for the Clojure SWI-Prolog bridge."}
  clj.swipl7.demo
  (:require [clj.swipl7.core :as plc]
            [clj.swipl7.protocols :as pl]) 
  (:use clojure.pprint clojure.repl))

#_(pl/set-traditional!)

;;------------------------------------------------------------------------
;; demo functions
;;------------------------------------------------------------------------

(defn demo-family
  []
  (plc/consult "resources/family.pl")
  (let [var-x     (pl/to-pl "X")
        atom-ralf (pl/to-pl "ralf")
        query     (pl/new-q-with-params "descendent_of" [var-x atom-ralf])
        solutions (pl/run-q query)]
    (println "demo-family : all solutions to " (pl/pl-to-text query)  " ==> ")
    (plc/show-solutions solutions)))

(defn demo-all-solutions
  []
  (let [query     (pl/new-q "X is 1; X is 2; X is 3")
        solutions (pl/run-q query)]
    (println "demo-all-solutions: all solutions to " (pl/pl-to-text query)  " ==> ")
    (plc/show-solutions solutions)))

(defn demo-n-solutions
  []
  (let [solutions (pl/run-q-n "X is 1; X is 2; X is 3" 2)]
     (println "demo-n-solutions: only first 2 solutions to previous query ==> ") 
     (plc/show-solutions solutions)))

(defn demo-append
  []
  (let [query    (pl/new-q "append([1, 2, 3], [4, 5], X).")
        solution (pl/run-q-1 query)]
    (println "demo-append / Prolog list processing. Query: " (pl/pl-to-text (plc/get-goal query))
             " ==>\n" (pl/get-val solution "X"))))

(defn demo-compounds
  []
  (let [compound  (plc/new-compound "append" [(pl/to-pl "Xs") 
                                              (pl/to-pl "Ys") 
                                              (pl/to-pl [(pl/to-pl "a") 
                                                         (pl/to-pl "b") 
                                                         (pl/to-pl "c")])])
        query     (pl/new-q compound)
        solutions (pl/run-q query)
        counter   (count solutions)]
   (println "demo-compounds: " (pl/pl-to-text query) " has " counter "solutions :")
   (plc/show-solutions solutions)))

(defn demo-lists
  []
  (let [pl-list (pl/to-pl [(pl/to-pl "a") (pl/to-pl "b") (pl/to-pl "c")])
        clj-list (plc/pl-list-to-clj-list pl-list)
        clj-vec (plc/pl-list-to-vec pl-list)]
    (println "demo-lists: prolog-list = " (pl/pl-to-text pl-list) 
             " ; as clj-list = " clj-list 
             " ; as clj-vec = " clj-vec))) 

(defn demo-with-params
  []
  (let [arg1 (pl/to-pl 11)
        arg2 (pl/to-pl 22)
        query-with-params (pl/new-q-with-params "X = ?, Y = ? ", [arg1 arg2])
        solution (pl/run-q-1 query-with-params)]
    (println "demo-with-params: query with params: " (pl/pl-to-text query-with-params) 
             " ==> ")
    (plc/show-solution solution)))

(defn demo-get-val
  []
  (let [arg1 (pl/to-pl 11)
        arg2 (pl/to-pl 22)
        query-with-params (pl/new-q-with-params "X = ?, Y = ? ", [arg1 arg2])
        solution (pl/run-q-1 query-with-params)]
    (println "demo-get-val: query with params: " (pl/pl-to-text query-with-params) 
             " ==> X resolved to " (pl/get-val solution "X") 
             ", Y resolved to " (pl/get-val solution "Y"))))

(defn demo-lib-version
  []
  (let [query    (pl/new-q "jpl_pl_lib_version(Version)")
        solution (pl/run-q-1 query)]
   (println "demo-lib-version : JPL version check: prolog jpl library version = "
                   (pl/get-val solution "Version")
            "; Java jpl library version = " (plc/get-jpl-version-as-string))))

(defn demo-prolog-version-int
  []
  (let [query   (pl/new-q "current_prolog_flag(version, Version)")
        solution (pl/run-q-1 query)]
   (println "demo-prolog-version: prolog version = "  (pl/get-val solution "Version"))))

(defn demo-prolog-version-data
  []
  (let [query    (pl/new-q "current_prolog_flag(version_data, Version)")
        solution (pl/run-q-1 query)]
   (println "demo-prolog-version: prolog version data = "  
            (plc/compound-to-text(pl/get-val solution "Version")))))
             

(defn demo-prolog-exception-1
  []
  (try 
    (pl/new-q "p(].") 
    (catch org.jpl7.JPLException exc 
      (println "demo-prolog-exception-1: goal 'p(].' ==> "
               (pl/pl-to-text (plc/get-term-from-exception exc))))))

(defn demo-prolog-exception-2
  []
  (plc/try-pl 
    (pl/has-solution? (str "consult('non_existing_file.pl')"))
    (println "demo-prolog-exception-2: file not found exception ==> "
               (pl/pl-to-text (plc/get-term-from-exception exc)))))

(defn demo-stats
  []
  (let [query (pl/new-q "statistics.")]
    (println "demo-stats : Getting SWI-Prolog statistics. Check console output.")
    (pl/run-q-1 query)))

;;------------------------------------------------------------------------
;; run the demos
;;------------------------------------------------------------------------

; this breaks (demo-stats) below!?!
#_(pl/set-traditional!)

(demo-family)
(demo-all-solutions)
(demo-n-solutions) 
(demo-append)
(demo-compounds)
(demo-lists)
(demo-with-params)
(demo-get-val)
(demo-lib-version)
(demo-prolog-version-int)
(demo-prolog-version-data)
(demo-prolog-exception-1)
(demo-prolog-exception-2)
(demo-stats)

;; EOF

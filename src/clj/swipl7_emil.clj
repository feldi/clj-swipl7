(ns 
  ^{:author "Peter Feldtmann"
    :doc "Demo code for the Clojure SWI-Prolog bridge."}
  clj.swipl7.emil
  (:require [clj.swipl7.core :as pl]) 
  (:use clojure.pprint clojure.repl))

;;------------------------------------------------------------------------
;; "emil" test function
;;------------------------------------------------------------------------

(defn emil2
  []
  (pl/consult "D:/ws/prolog/emil/emilTests.pl")
  (let [query     (pl/new-q "test2(Variante).")
        solutions (pl/run-q query)]
    (println "emil " (pl/pl-to-text query)  " ==> " )
              (pl/show-solutions solutions)))

(defn emil3
  []
  (pl/consult "D:/ws/prolog/emil/emilTests.pl")
  (let [query     (pl/new-q "test3(Variante).")
        solutions (pl/run-q query)]
    (println "emil " (pl/pl-to-text query)  " ==> " )
              (pl/show-solutions solutions)
              ))
  
;;------------------------------------------------------------------------
;; run emil
;;------------------------------------------------------------------------

(emil2)
(emil3)

;; EOF

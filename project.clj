(defproject clj-swipl7 "0.1.1-SNAPSHOT"
  :description "A Clojure library designed to call SWI-Prolog 7 goals directly from clojure code"
  :url "https://github.com/feldi/clj-swipl7"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :resource-paths ["resources"] 
  :aot [clj.swipl7.JPLExtend]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [jpl7/jpl7 "7.4.0"]])
                 

(defproject clj-swipl7 "0.1.1"
  :description "Clojure SWI-Prolog bridge"
  :url "https://github.com/feldi/clj-swipl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :resource-paths ["resources"] 
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ;;[jpl/jpl "3.1.4-alpha"]
                  [jpl7/jpl7 "7.0.1-alpha"]
                 ])

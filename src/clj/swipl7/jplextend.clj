(ns 
  ^{:author "Peter Feldtmann"
    :doc "Experimental.
          Expose protected Java methods as public.
          Doesnt work for static protected !?"}
  clj.swipl7.JPLExtend
  (:gen-class
    :name clj.swipl7.JPLExtend
    :extends org.jpl7.JPL
    ;;:methods [[getQuotedName [String] String]]
    :exposes-methods {quotedName parentQuotedName}))

(defn -quotedName 
  [name]
  (.parentQuotedName org.jpl7.JPL name)) ;; ? doesnt work


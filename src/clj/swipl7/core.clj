(ns 
  ^{:author "Peter Feldtmann"
    :doc "A Clojure SWI-Prolog bridge.
          Call prolog goals directly from clojure code."}
  clj.swipl7.core
  (:use [clj.swipl7.protocols])
  (:require [clojure.string :as str])
  (:import [org.jpl7 JPL Atom Compound JPLException
            PrologException Query Term
            Util Variable JRef Version]
           [clojure.lang BigInt]
           [java.math BigInteger]))

; to install the SWI-Prolog java bridge jpl.jar:
; get and install the leiningen plugin 'localrepo',
; then do: lein localrepo install 'path-to-swi-prolog'/lib/jpl.jar jpl7 7.4.0 
; (or whatever version is the latest)

(set! *warn-on-reflection* true)

;; some constants
(def ^:const j-false JPL/JFALSE)
(def ^:const j-true JPL/JTRUE)
(def ^:const j-null JPL/JNULL)
(def ^:const j-void JPL/JVOID)

;; Atom types, pre-defined
(def ^:const atom-type-text "text")
(def ^:const atom-type-string "string")
(def ^:const atom-type-reserved-symbol "reserved_symbol")

;; Term types from jpl/fli/Prolog.java
(def ^:const pl-variable org.jpl7.fli.Prolog/VARIABLE)
(def ^:const pl-atom org.jpl7.fli.Prolog/ATOM)
(def ^:const pl-integer org.jpl7.fli.Prolog/INTEGER)
(def ^:const pl-float org.jpl7.fli.Prolog/FLOAT)
(def ^:const pl-string org.jpl7.fli.Prolog/STRING)
(def ^:const pl-compound org.jpl7.fli.Prolog/COMPOUND)
(def ^:const pl-list-nil org.jpl7.fli.Prolog/LIST_NIL)
(def ^:const pl-list-pair org.jpl7.fli.Prolog/LIST_PAIR)
(def ^:const pl-jboolean org.jpl7.fli.Prolog/JBOOLEAN)
(def ^:const pl-jref org.jpl7.fli.Prolog/JREF)
(def ^:const pl-jvoid org.jpl7.fli.Prolog/JVOID)
(def ^:const pl-dict org.jpl7.fli.Prolog/DICT)
(def ^:const pl-blob org.jpl7.fli.Prolog/BLOB)

(def ^:const pl-succeed org.jpl7.fli.Prolog/succeed)
(def ^:const pl-fail org.jpl7.fli.Prolog/fail)


;; empty list and pair (can change: traditional or modern syntax)
(defn list-nil  [] JPL/LIST_NIL)
(defn list-pair [] JPL/LIST_PAIR)


;; forward declarations
(declare new-compound term)


;; Helpers

(defn- string-starts-with-upper-case
  [^String s]
   (Character/isUpperCase (.codePointAt s 0))) 

(defn- keywordize 
  [s]
  (if (keyword? s)
    s
    (-> (name s) 
      str/lower-case
      (str/replace "_" "-")
      (str/replace "." "-")
      (keyword))))


;; JPL main class

(defn set-native-library-name! 
   "Set the name of the native library, e.g. \"jpl.dll\" on Windows."
  [^String name]
  (JPL/setNativeLibraryName name))

(defn set-native-library-dir! 
  "Set the directory name of the native library."
  [^String dir]
  (JPL/setNativeLibraryDir dir))

(defn set-native-library-path! 
   "Set the directory\name as path to the native library."
  [^String path]
  (JPL/setNativeLibraryPath path))

(defn load-native-library
  "Load the native library, e.g. jpl.dll on Windows."
  []
  (JPL/loadNativeLibrary))

(defn ^String get-jar-path
  "Get the (absolute) path of the used jpl.jar file." 
  []
  (JPL/jarPath))

(defn get-jpl-version-as-string
  "Get jpl version as string, e.g. '7.0.1-alpha'." 
  []
  (JPL/version_string))

(defn ^Version get-jpl-version
  "Get jpl version as version object. Alas, this object is package private..." 
  []
  (JPL/version))

(defn init
  "Force explicit initialization using the default init parameters, 
   or provided as a vector of strings."
  ([]
    (JPL/init))
  ([args-list]
    (JPL/init (into-array String args-list))))

(defn is-initialized?
  "Checks if Prolog VM is initialized."
  []
  (nil? (JPL/getActualInitArgs)))

(defn get-default-init-args
  []
  (into [] (JPL/getDefaultInitArgs)))

(defn set-default-init-args!
  "Set init parameters to be used when the prolog engine is not
   explicitly initialized, i.e. automatically at the first query open.
   Provide the parameters as a vector of strings." 
  [args-list]
  (JPL/setDefaultInitArgs (into-array String args-list)))

(defn get-actual-init-args
  "Returns the sequence of command-line arguments
	 that were actually used when the Prolog engine was formerly initialised.
	 
	 This method returns null if the Prolog engine has not yet been
	 initialised, and thus may be used to test this condition."
  []
  (into [] (JPL/getActualInitArgs)))

(defn is-initialized?
  "Checks if Prolog VM is initialized."
  []
  (not (empty? (get-actual-init-args))))

(defn halt
  "Terminates the Prolog session."
  []
  (JPL/halt))

(defn set-dtm-mode!
  "Set or unset 'dont-tell-me' mode."
  [^Boolean mode]
  (JPL/setDTMMode mode))

(defn set-traditional!
  "switch to traditional Prolog syntax, like using the --traditional arg.
   See also function 'traditional?'."
  []
  (JPL/setTraditional))

(defn set-traditional-anyway!
  "switch always to traditional Prolog syntax, like using the --traditional arg.
  See also function 'traditional?'."
  []
  (JPL/setTraditionalAnyway))

(defn ^String get-syntax 
  []
  (JPL/getSyntax))

(defn ^Term new-jref 
  [o]
  (JPL/newJRef o))

#_(def JPLProxy
   (proxy [org.jpl7.JPL] []
    (quotedName [this n]
      (proxy-super org.jpl7.JPL/quotedName n))))


;; Util

(defn ^Term text-to-term
  "Converts a Prolog source text to a Term."
  [^String text]
  (Util/textToTerm text))

(defn ^Term text-with-params-to-term
   "Converts a Prolog source text to a Term, with replacing successive
    occurences of '?' in the text by the corresponding term from the args list."
  [^String text args]
  (Util/textParamsToTerm text (into-array Term  args)))

(defn ^Term seq-to-pl-list
  [terms]
  (Util/termArrayToList (into-array Term terms)))

(defn ^Term string-list-to-pl-list
  [list-of-strings]
  (Util/stringArrayToList (into-array String list-of-strings)))

(defn ^Term int-list-to-pl-list
  [list-of-ints]
  (Util/intArrayToList (int-array list-of-ints)))

(defn ^Term int-list-list-to-pl-list
  [list-of-list-of-ints]
  (Util/intArrayArrayToList list-of-list-of-ints))

(defn ^boolean is-pl-list
  "whether the Term represents a proper list."
  [term]
  (Util/isList term))

(defn pl-list-to-length
  "The length of the proper list which the Term represents, else -1 ."
  [term]
  (Util/listToLength term))

(defn pl-list-to-vec
  [term]
  (mapv #(to-clj %) (Util/listToTermArray term)))

(defn pl-list-to-clj-list
  [term]
  (map #(to-clj %) (Util/listToTermArray term)))

(defn pl-atom-list-to-string-list
  [^Term term]
  (into [] (Util/atomListToStringArray term)))

(defn ^java.util.Map term-to-bindings
  [ ^Term term]
  (Util/namevarsToMap term))

(defn ^String solution-to-text
  "convert solution hash map to string."
  [^java.util.Map solution-map]
  (Util/toString solution-map)) 

(defn show-solution
  "Pretty print a solution hash map."
  [^java.util.Map solution-map]
  (doall(doseq [[varname term] solution-map]
          (println varname " = "  term)))) 

(defn show-solutions
  "Pretty print a list of solutions."
  [solution-list]
  (doall (doseq [soln solution-list] (show-solution soln))))

(defn seq-to-term-list
  "Makes of any sequence a list of terms."
  [s]
  (map term s))


;; Exceptions

(defn get-term-from-exception 
  [^PrologException exc]
  (.term exc))

(defmacro try-pl 
  [pl-form exception-form]
  `(try 
     ~pl-form
    ;; (catch org.jpl7.JPLException ~'exc
     (catch Exception ~'exc
       ~exception-form)))


;; Atom

(defn new-atom
  ([^String name]
    (Atom. name))
  ([^String name ^String type]
    (Atom. name type)))

(defn get-atom-name
  "the name (unquoted)."
  [^Atom atom]
  (.name atom)) 

(defn get-atom-type
   "returns the type of an atom"
  [^Atom atom]
  (.atomType atom)) 

(defn get-atom-pl-type
   "returns the prolog type of an atom, as 'Prolog.ATOM'"
  [^Atom atom]
  (.type atom)) 

(defn get-atom-type-name
  "returns the name of the type of an atom, as 'Atom'"
  [^Atom atom]
  (.typeName atom)) 

(defn atom-is-text?
  [^Atom atom]
  (= (get-atom-type atom atom-type-text)))
  
(defn atom-is-string?
  [^Atom atom]
  (= (get-atom-type atom atom-type-string)))

(defn atom-is-reserved-symbol?
  [^Atom atom]
  (= (get-atom-type atom atom-type-reserved-symbol)))
    
(defn new-empty-list
  "Build an empty prolog list."
  []
  (list-nil)) 

(defn empty-list?
  "checks for empty prolog list. Old style."
  [^Atom a]
  (= (.name a) "[]")) 

(defn atom-is-list-nil?
  "Checks whether atom denotes (syntax-specifically) an empty list."
  [^Atom a]
  (.isListNil a)) 

(defn atom-to-text
  "An Atom's name is quoted if it is not a simple identifier."
  [^Atom atom]
  (.toString atom)) 


;; Variable

(defn new-var
  "Create a new Variable with new sequential name of the form '_261'."
  ([] (Variable.))
  ; Create a new Variable with 'name' (which must not be null or ""),
  ; and may one day be constrained to comply with traditional Prolog syntax.
  ([var-name]
    (Variable. (clojure.string/upper-case (name var-name)))))

(defn new-anon-var
  "Create a new anonymous Variable."
  [] (Variable. "_"))

(defn get-var-name
  " the lexical name of a Variable."
  [^Variable var]
  (.name var)) 

(defn var-to-text
  "Pretty print variable." 
  [^Variable var]
  (.toString var)) 

(defn get-var-pl-type
   "returns the prolog type of a variable, as 'Prolog.VARIABLE'"
  [^Variable var]
  (.type var)) 

(defn get-var-type-name
  "returns the name of the type of a variable, as 'Variable'"
  [^Variable var]
  (.typeName var)) 


;; JRef

(defn new-jref
  [obj]
  (JRef. obj)) 

(defn get-object
  [^JRef ref]
  (.object ref))

(defn get-jref-pl-type
   "returns the prolog type of a JRef, as 'Prolog.JREF'"
  [^JRef jref]
  (.type jref)) 

(defn get-jref-type-name
  "returns the name of the type of a JRef, as 'JRef'"
  [^JRef jref]
  (.typeName jref)) 


;; Integer

(defn new-integer
  [l]
  (org.jpl7.Integer. (long l)))

(defn new-big-integer
  [ big]
  (org.jpl7.Integer. (biginteger big)))

(defn get-int-as-double
  [^org.jpl7.Integer i]
  (.doubleValue i))

(defn get-int-as-float
  [^org.jpl7.Integer i]
  (.floatValue i))

(defn get-int-value
  [^org.jpl7.Integer i]
  (.intValue i))

(defn get-int-as-long
  [^org.jpl7.Integer i]
  (.longValue i))

(defn get-big-value
  [^org.jpl7.Integer i]
  (.bigValue i))

(defn is-big?
  [^org.jpl7.Integer i]
  (.isBigInteger i))

(defn get-int-or-big-value
  [^org.jpl7.Integer i]
  (if (is-big? i)
  (.bigValue i)
  (.intValue i)))

(defn int-to-text
  [^org.jpl7.Integer i]
  (.toString i))

(defn get-int-pl-type
   "returns the prolog type of an Integer, as 'Prolog.INTEGER'"
  [^org.jpl7.Integer i]
  (.type i)) 

(defn get-int-type-name
  "returns the name of the type of an Integer, as 'Integer'"
  [^org.jpl7.Integer i]
  (.typeName i)) 


;; Float

(defn new-float
  [^double d]
  (org.jpl7.Float. d))

(defn get-float-as-double
  [^org.jpl7.Float flt]
  (.doubleValue flt))

(defn get-float-value
  [^org.jpl7.Float flt]
  (.floatValue flt))

(defn get-float-as-int
  "returns the (double) value of a Float, converted to an int"
  [^org.jpl7.Float flt]
  (.intValue flt))

(defn get-float-as-long
  "returns the (double) value of a Float, converted to an long"
  [^org.jpl7.Float flt]
  (.longValue flt))

(defn float-to-text
  [^org.jpl7.Float flt]
  (.toString flt))

(defn get-float-pl-type
   "returns the prolog type of a Float, as 'Prolog.FLOAT'"
  [^org.jpl7.Float flt]
  (.type flt)) 

(defn get-float-type-name
  "returns the name of the type of a Float, as 'Float'"
  [^org.jpl7.Float flt]
  (.typeName flt)) 


;; Term

(defn term
  "Convenience method for term construction."
  ([]
    ;; anonymous var
    (new-anon-var))
  ([s]
    ;; Atom, Variable or List
    (to-pl s))
  ([s1 s2]
    ;; compound term
    (new-compound (name s1) s2)))

 (defn term-to-text
   "Pretty format a term." 
  [^Term term]
  (.toString term))
  
(defn term-list-to-text
  "Pretty format a list of terms." 
  [terms]
  (Term/toString (into-array Term terms)))

(defn ^String get-name
  [^Term term]
  (.name term))

(defn ^int get-arity
  [^Term term]
  (.arity term))

(defn ^int get-type
  "See term types." 
  [^Term term]
  (.type term))

(defn ^String get-term-atom-type
  "Defined only for Atoms." 
  [^Term term]
  (.atomType term))

(defn ^String get-type-name
  [^Term term]
  (.typeName term))

(defn has-functor?
  "Whether the compounds functor has name and arity."
  [^Term term ^String name arity]
  (.hasFunctor term name (long arity)))

(defn has-functor-integer?
  "Whether the compounds functor is integer and arity."
  [^Term term i arity]
  (.hasFunctor term (int i) (long arity))) 

(defn has-functor-double?
  "Whether the compounds functor is double and arity."
  [^Term term d arity]
  (.hasFunctor term (double d) (long arity))) 


; some special functors
(def list-functor ".")
(def pair-functor "[|]")
(def and-functor ",")
(def or-functor ";")
(def if-functor "->")
(def jref-functor "@")

(defn ^Term get-arg
  "get the ith argument (counting from 1)." 
  [^Term c i]
  (.arg c (long i)))

(defn get-args
  "get all arguments." 
  [^Term c]
  (into [] (.args c)))

(defn get-int-value
  [^Term term]
  (.intValue term))

(defn get-big-value
  [^Term term]
  (.bigValue term))

(defn get-long-value
  [^Term term]
  (.longValue term))

(defn get-float-value
  [^Term term]
  (.floatValue term))

(defn get-double-value
  [^Term term]
  (.doubleValue term))

(defn is-atom? 
  "whether this Term is an Atom (of any type)."
  [^Term term]
  (.isAtom term)) 

#_(defn is-atom-of-name-type? 
   "Tests whether this Term is an Atom with name and type."
  [^Term term ^String atom-name ^String atom-type]
  (.isAtomOfNameType term atom-name atom-type)) 

(defn is-compound? 
  "Tests whether this Term is a Compound."
  [^Term term]
  (.isCompound term)) 

(defn is-float? 
  [^Term term]
  (.isFloat term)) 

(defn is-integer? 
  [^Term term]
  (.isInteger term)) 

(defn is-big-integer? 
  [^Term term]
  (.isBigInteger term)) 

(defn is-var? 
  [^Term term]
  (.isVariable term)) 

(defn is-list-nil? 
   "Checks whether atom denotes (syntax-specifically) an empty list."
  [^Term term]
  (.isListNil term)) 

(defn is-list-pair? 
   "Checks whether atom denotes (syntax-specifically) a pair."
  [^Term term]
  (.isListPair term)) 

(defn is-jref?
  [^Term term]
  (.isJRef term))

(defn is-j-false?
  [^Term term]
  (.isJFalse term))

(defn is-j-true?
  [^Term term]
  (.isJTrue term))

(defn is-j-null?
  [^Term term]
  (.isJNull term))

(defn is-j-void?
  [^Term term]
  (.isJVoid term))

(defn is-list-nil?
  [^Term term]
  (.isListNil term))

(defn is-list-pair?
  [^Term term]
  (.isListPair term))

(defn is-list?
  [term]
  (or (has-functor? term list-functor 2)
      (has-functor? term pair-functor 2)))
  
(defn is-pair?
  [term]
  (has-functor? term pair-functor 2))
    
(defn is-and?
  [term]
  (has-functor? term and-functor 2))

(defn is-or?
  [term]
  (has-functor? term or-functor 2))

(defn is-if?
  [term]
  (has-functor? term if-functor 3))

(defn length-of-pl-list
  "Iff term is a prolog list, return its length." 
  [^Term term]
  (.listLength term)) 

(defn to-term-array
  "Iff term is a prolog list, return an array of its succcessive members." 
  [^Term term]
  (.toTermArray term)) 

(defn to-term-vec
  "Iff term is a prolog list, return a clojure vector of its succcessive members." 
  [^Term term]
  (vec (.toTermArray term))) 


;; Compound

(defn new-compound
  ([^String name] 
    (Compound. name))
  ([^String name args]
    (Compound. name ^"[Lorg.jpl7.Term;" (into-array Term (seq-to-term-list args)))))

; made proteced
;(defn compound-with-arity
;  [^String name ^Integer arity]
;    (Compound. name arity))

(defn set-arg
  "Set the i-th (counting from 1) arg of a compound."
  [^Compound c ^Integer index ^Term term]
  (.setArg c index term)) 

(defn compound-to-text
  [^Compound c]
  (.toString c))


;; Query

(defn ^Term get-goal
  "Returns the term representing the goal of the query." 
  [^Query q]
  (.goal q))

(defn iterator
  "A Query is its own Iterator." 
  [^Query q]
  (.iterator q))

(defn ^boolean has-next?
  "whether this Query has a (further) solution." 
  [^Query q]
  (.hasNext q))

(defn ^java.util.Map get-next
  "this Query's next solution." 
  [^Query q]
  (.next q))

(defn ^java.util.Map get-solution
  "Returns the first solution." 
  [^Query q]
  (.getSolution q))

;; deprecated ?
;(defn ^boolean has-more-solutions?
;  "Returns true is the query succeeds, otherwise false." 
;  [^Query q]
;  (.hasMoreSolutions q))

;; deprecated ?
;(def has-more-elements?
;  "Alias for java.util.Enumeration interface compliance."
;  has-more-solutions?) 

;; deprecated ?
;(defn get-next-solution
;  "Returns the next solution. Check with has-more-solutions? before." 
;  [^Query q]
;  (.nextSolution q))

;; deprecated ?
;(def next-element 
;  "Alias for java.util.Enumeration interface compliance."
;  get-next-solution) 

(defn get-all-solutions
  "Calls the Query's goal to exhaustion and returns an array of zero or more 
   Maps of zero or more variablename-to-term bindings (each Map represents 
   a solution, in the order in which they were found)." 
  [^Query q]
  (.allSolutions q))

(defn get-n-solutions
  "calls the Query's goal to exhaustion or until N solutions are found, 
   whichever is sooner, and returns an array containing (as possibly empty
   Maps of variablename-to-term bindings) every found solution (in the order 
   in which they were found)."
  [^Query q n]
  (.nSolutions q n))

(defn get-one-solution
  "Returns the first solution, if any, as a (possibly empty) Map of 
   variablename-to-term bindings, else null."
  [^Query q]
  (.oneSolution q))

(defn ^boolean has-a-solution?
  "Returns the provability of the Query, i.e. 'true' if it has at least one
   solution, 'false' if the call fails without finding a solution.
	 Only the first solution (if there is one) will be found; any bindings 
   will be discarded, and the Query will be closed."
  [^Query q]
  (.hasSolution q))


(defn get-subst-with-name-vars
  "Returns the first solution with name-variable-substitutions.
   Assumes that the query's last argument is a variable which will be bound
   to a [name=Var,...] dictionary." 
  [^Query q]
  (.getSubstWithNameVars q))

(defn get-raw-value 
  "Returns the 'raw' value of a variable."
  [solution var]
  (if (instance? Variable var)
    (get solution (get-var-name var))
    (get solution var)))

(defn get-clj-value 
  "Returns the 'clojurized' value of a variable."
  [solution var]
  (if (instance? Variable var)
    (get solution (keywordize (get-var-name var)))
    (get solution (keywordize var))))

(defn open-query
  [^Query q]
  (.open q))

(defn close-query
  "You may close an open query before its solutions are exhausted." 
  [^Query q]
  (.close q))

(defn is-open?
  "Returns true iff the query is open." 
  [^Query q]
  (.isOpen q))

(defn query-to-text
  "A crude string representation of a query." 
  [^Query q]
  (.toString q))

(defn show-query
  "A representation of a query." 
  [^Query q]
  (to-clj (get-goal q)))


;; Convenience methods

(defn consult
  "Consult and run a prolog source file." 
  [file]
  (try-pl 
    (if (has-solution? (str "consult('" file "')"))
      :ok
      (str "Failed to consult " file))
    (str "caught exception: " (get-term-from-exception  exc)))) 

(defn consult-from-string
  "Consult and run a prolog source strimg." 
  [s]
  (let [tempFile (java.io.File/createTempFile "pl_consult_from_string_" ".pl")
        tempFileName (clojure.string/replace (.getAbsolutePath tempFile) #"\\" "/")
        _ (with-open [file (clojure.java.io/writer tempFile)]
            (binding [*out* file]
              (println s)))
        res (consult tempFileName )]
    (.delete tempFile)
    res)) 

(defn halt-prolog 
  "Stops the prolog engine." 
  []
   (run-q-1 "halt.")) 

(defn ^boolean traditional?
  []
  (-> (new-q "current_prolog_flag(traditional, Trad)")
      get-one-solution 
      (get-val "Trad")
      (= "true")))

(defn ^String get-prolog-home
  []
  (-> (new-q "current_prolog_flag(home, Home)")
      get-one-solution 
      (get-val "Home")))

(defn ^String get-prolog-version-as-int
  []
  (-> (new-q "current_prolog_flag(version, Version)")
      get-one-solution 
      (get-val "Version")))

(defn get-prolog-version-as-map
  "The returned map with 'clojurized' keys may look like this:
   {:major 7, :minor 2, :patch 3, :extra []}  "
  []
  (to-clj (run-q-1 
           "current_prolog_flag(version_data, swi(Major, Minor, Patch, Extra))")))
   
(defn ^String get-jpl-prolog-version
  []
  (-> (new-q "jpl_pl_lib_version(Version).")
      get-one-solution 
      (get-val "Version")))


;; protocol implementations

(extend-protocol IPrologSourceText
  
  nil
  
  (text-to-pl [text]
    (new-empty-list))
  
  (pl-to-text [term]
    "")
   
  String
  
  (text-to-pl [text]
    (Util/textToTerm text))
  
  org.jpl7.Compound
  
  (pl-to-text [term]
    (.toString term))
  
  org.jpl7.Term
  
  (pl-to-text [term]
    (.toString term))
  
  org.jpl7.Query
  
  (pl-to-text [query]
    (pl-to-text (get-goal query)))
  
  java.util.Map
  
  (pl-to-text [soln]
    (Util/toString soln))

  Object
  
  (pl-to-text [lis]
    (.toString lis))
 )


(extend-protocol ICljToPrologConversion
  
  nil
  
  ;; "old LISPy": nil becomes the empty prolog list!
  (to-pl [data]
    (new-empty-list))
  
  clojure.lang.PersistentList$EmptyList
  
  (to-pl [data]
    (new-empty-list))
  
  String
  
  (to-pl [data]
    (if (string-starts-with-upper-case data)
      (new-var data) 
      (new-atom data)))
  
  clojure.lang.Symbol
  
  (to-pl [data] 
    (to-pl (name data)))
  
  clojure.lang.Keyword
  
  (to-pl [data] 
    (to-pl (name data)))
    
  clojure.lang.IPersistentVector
  
  (to-pl [data] 
    (seq-to-pl-list (map to-pl data)))
  
  clojure.lang.IPersistentList
  
  (to-pl [data] 
    (seq-to-pl-list (map to-pl data)))
  
  clojure.lang.IPersistentMap
  
  (to-pl [data] 
    (seq-to-pl-list (map to-pl data))) ;;?!? Dict...
  
  java.lang.Integer
  
  (to-pl [data] 
    (new-integer data))
  
  java.lang.Long
  
  (to-pl [data]
    (new-integer data))
  
  java.lang.Float
  
  (to-pl [data]
    (new-float data))
  
  java.lang.Double
  
  (to-pl [data]
    (new-float data))
  
  java.math.BigInteger
  
  (to-pl [data]
    (new-big-integer data))
  
  clojure.lang.BigInt
  
  (to-pl [data] 
    (new-big-integer (bigint data)))
  
  Term
  
  (to-pl [data]
    data)
  
  Object
  
  (to-pl [data] 
    (new-jref data)))


(extend-protocol IPrologToCljConversion
  
  nil
  
  (to-clj [data]
    nil)
  
  org.jpl7.Integer
  
  (to-clj [data]
    (get-int-or-big-value data))
  
  org.jpl7.Float
  
  (to-clj [data]
    (get-float-value data))
  
  org.jpl7.Atom
  
  (to-clj [data] 
    (if (atom-is-list-nil? data) 
      '()
      (get-name data)))
  
  org.jpl7.Variable
  
  (to-clj [data]
    (get-var-name data))
  
  org.jpl7.JRef
  
  (to-clj [data]
    (get-object data))
    
  ;; TODO
;  org.jpl7.Compound
;  (to-clj [data] data)) 

  org.jpl7.Term
  
  (to-clj [data] 
    (if (is-list? data)
      (pl-list-to-clj-list data)
      data))
  
  java.util.Map
  
  (to-clj [data] 
    ;; convert Prolog variable names to clojure keywords
    (zipmap (for [[^String k] data] (keywordize k))
            (for [[_ v]       data] (to-clj v))))
  
  clojure.lang.PersistentVector
  
  (to-clj [data] 
    (mapv to-clj data))
  
  Object
  
  (to-clj [data]
    data))


(extend-protocol IPrologQuery
  
  String
  
  (new-q [text]
    (Query. text))
  
  (new-q-with-param [text ^Term param]
    (Query. text param))
  
  (new-q-with-params [text params]
    (Query. text ^"[Lorg.jpl7.Term;" (into-array Term params)))
  
  (has-solution? [text]
    (Query/hasSolution text))
  
  (has-solution-with-params? [text params]
    (Query/hasSolution text (into-array Term params)))
  
  (run-q-1 [text]
    (to-clj (Query/oneSolution text)))
  
  (run-q-1-with-params [text params]
    (to-clj (Query/oneSolution text (into-array Term params))))
  
  (run-q [text]
    (to-clj (vec (Query/allSolutions text))))
  
  (run-q-with-params [text params]
    (to-clj (vec (Query/allSolutions text (into-array Term params)))))
  
  (run-q-n [text n]
    (to-clj (vec (Query/nSolutions text (long n)))))
  
  (run-q-n-with-params [text params n]
    (to-clj (vec (Query/nSolutions text (into-array Term params) (long n)))))
  
  org.jpl7.Term
  
  (new-q [term]
    (Query. term))
  
  (has-solution? [term]
    (Query/hasSolution term))
  
  (run-q-1 [term]
    (to-clj (Query/oneSolution term)))
  
  (run-q [term]
    (to-clj (vec (Query/allSolutions term))))
  
  (run-q-n [term n]
    (to-clj (vec (Query/nSolutions term (long n)))))
  
  org.jpl7.Query
  
  (has-solution? [q]
    (.hasSolution q))
  
  (run-q-1 [q]
    (to-clj (.oneSolution q)))
  
  (run-q [q]
    (to-clj (vec (.allSolutions q))))
  
  (run-q-n [q n]
    (to-clj (vec (.nSolutions q (long n)))))
)


(extend-protocol IPrologSolution
  
  nil
  
  (get-val [soln var]
    nil)
  
  (success? [soln]
    false)
  
  (ground-success? [soln]
    false)
  
  (failed? [soln]
    true)
  
  java.util.Map
  
  (get-val [soln var]
    (get-clj-value soln var)
    #_(when-let [value (get-clj-value soln var)]
      (to-clj value)))
  
  (success? [soln]
    true)
  
  (ground-success? [soln]
    (empty? soln))
  
  (failed? [soln]
    false)
)

(extend-protocol IPrologSolutionList
  
  nil
  
  (success? [soln]
    false)
  
  (failed? [soln]
    true)
  
  clojure.lang.PersistentVector
  
  (has-soln? [soln]
    (not (empty? soln)))
  
  (has-no-soln? [soln]
    (empty? soln))
  )

;; EOF

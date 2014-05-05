(defn make-tmp-dir!
  "Creates a directory in directory d and returns its name."
  [d]
  (let [tmp (java.io.File. d (format "/class-cache/cache-%s" (System/currentTimeMillis)))]
    (or (.mkdir tmp)
        (throw (RuntimeException. (format "Make dir '%s' failed." tmp))))
    (str tmp)))

(defn delete-tree!
  "Deletes the directory d and all its files and sub-directories."
  [d]
  (let [f (java.io.File. (str d))]
    (when (.isDirectory f)
      (doseq [c (.listFiles f)]
        (delete-tree! c)))
    (.delete f)))

(defn files
  "Returns a Seq<File> of all files in directory d and its
sub-directories."
  [d]
  (flatten 
   (for [f (.listFiles (java.io.File. (str d)))]
     (if (.isDirectory f)
       (files f)
       f))))

(defn class-name
  "Returns the classname that corresponds to the file f
below directory d."
  [d f]
  (let [cn (.substring (str f) (inc (.length (str d))))
        cn (.replace cn "/" ".")
        cn (.substring cn 0 (- (.length cn) 6))]
    cn))

(defn class-files
  "Returns Seq<[class-name file-name]> for all *.class files below
directory d."
  [d]
  (let [d (java.io.File. d)]
    (for [f (files d)
          :when (.endsWith (str f) ".class")
          :let [n (class-name d f)]]
      [n f])))

(defn class-def-for** [rec-fn body-fn]
  (let [tmp-dir (make-tmp-dir! (System/getProperty "java.io.tmpdir"))]
    (try
      (with-bindings {Compiler/COMPILE_FILES true
                      Compiler/COMPILE_PATH tmp-dir}
        (body-fn))
      (doseq [[n f] (class-files tmp-dir)
              :let [_ (.println System/out (format "CLASSFILE**: %s %s" n f))
                    bites (copy-io! f)]]
        (rec-fn n bites))
      (finally
        (delete-tree! tmp-dir)))))

(defn class-def-for*
  "Runs the Clojure compiler on the body/form and calls rec-fn
class-name class-bytes) for all resulting class-definitions."
  [rec-fn body]
  (let [tmp-dir (make-tmp-dir! (System/getProperty "java.io.tmpdir"))]
    (try
      (with-bindings {Compiler/COMPILE_FILES true
                      Compiler/COMPILE_PATH tmp-dir}
        (Compiler/analyze clojure.lang.Compiler$C/EXPRESSION body))
      (doseq [[n f] (class-files tmp-dir)
              :let [_ (.println System/out (format "CLASSFILE*: %s %s" n f))
                    bites (copy-io! f)]]
        (rec-fn n bites))
      (finally
        #_ (delete-tree! tmp-dir)))))

(defmacro class-def-for
  "Runs the Clojure compiler on the body/form and calls rec-fn
class-name class-bytes) for all resulting class-definitions.  Returns
the body/form. rec-fn must be a symbol which resolves to the
function."
  [rec-fn & body]
  (class-def-for* @(resolve rec-fn) body)
  `(do ~@body))

(defn caching-rec-fn [class-name class-bytes]
  (.println System/out (format "GRABBING class definition for '%s'." class-name))
  (put-cache class-name class-bytes))


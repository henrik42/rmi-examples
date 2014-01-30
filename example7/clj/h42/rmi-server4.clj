(def url-cl
  (let [r (java.net.URLClassLoader.
           (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
    (.println System/out (str "URL Classloader : " r))
    r))

(def dyn-cl
  (let [r (clojure.lang.DynamicClassLoader. url-cl)]
    r))

#_ (.bindRoot Compiler/LOADER dyn-cl)

#_
(defmacro with-cl [body]
  `(~@body))

#_
(let []
  (.print System/out "xx")
  (.setContextClassLoader (Thread/currentThread) dyn-cl)
  (.println System/out foo.MyService))

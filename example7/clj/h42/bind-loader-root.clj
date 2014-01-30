(let [url-cl (java.net.URLClassLoader.
              (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
      dyn-cl (clojure.lang.DynamicClassLoader. url-cl)]
  (.bindRoot Compiler/LOADER dyn-cl))

#_
(let [ctx-cl (-> (Thread/currentThread) (.getContextClassLoader))
      url-cl (java.net.URLClassLoader.
              (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
      res (enumeration-seq (.getResources url-cl "META-INF"))]
  res)
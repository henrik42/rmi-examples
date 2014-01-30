(def ctccl
  (let [cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
    (.println System/out (str "Setting context classloader to " cl))
    (.setContextClassLoader (Thread/currentThread) cl)
    cl))

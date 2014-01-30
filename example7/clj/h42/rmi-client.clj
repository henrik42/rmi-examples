(defn run-rmi-client []
  (let [rmi-reg (java.rmi.registry.LocateRegistry/getRegistry 1099)
        cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
    (.setContextClassLoader (Thread/currentThread) cl)
    (.voidMethod (.lookup rmi-reg "RmiExample7.MyService"))))

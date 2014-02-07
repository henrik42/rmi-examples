(defn run-rmi-client []
  (let [rmi-reg (java.rmi.registry.LocateRegistry/getRegistry 1099)
        _ (.println System/out (format "Using RMI registry %s" rmi-reg))
        cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
    (.setContextClassLoader (Thread/currentThread) cl)
    (let [stub (.lookup rmi-reg "RmiExample7.MyService")]
      (.println System/out (format "Calling .voidMethod on %s" stub))
      (.voidMethod stub))))

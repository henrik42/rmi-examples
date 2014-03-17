(defn run-rmi-client [& {:keys [r-csf host port] :or {host "127.0.0.1" port 1099}}]
  (let [rmi-reg (java.rmi.registry.LocateRegistry/getRegistry host port r-csf)
        _ (.println System/out (format "Using RMI registry %s" rmi-reg))
        cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
    (.setContextClassLoader (Thread/currentThread) cl)
    (let [stub (.lookup rmi-reg "RmiExample7.MyService")]
      (.println System/out (format "Calling .voidMethod on %s" stub))
      (.voidMethod stub))))

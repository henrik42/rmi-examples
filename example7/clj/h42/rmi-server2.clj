(defn run-rmi-server2 []
  (let [cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
        _ (.setContextClassLoader (Thread/currentThread) cl)
        impl (proxy [foo.MyService] []
               (voidMethod []
                 (.println System/out "executing voidMethod")))
        stub (java.rmi.server.UnicastRemoteObject/exportObject impl 6666)
        rmi-reg (java.rmi.registry.LocateRegistry/createRegistry 1099)]
    (.rebind rmi-reg "RmiExample7.MyService" stub)
    (.println System/out "RMI server is waiting for incoming calls...")))
   

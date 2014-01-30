(defmacro compile-with-cl [body]
  (.addURL @Compiler/LOADER (java.net.URL. "http://127.0.0.1:8080/class-server/"))
  `(~@body))

(defn run-rmi-server3 []
  (let [impl (compile-with-cl
               (proxy [foo.MyService] []
                 (voidMethod []
                   (.println System/out "executing voidMethod"))))
        stub (java.rmi.server.UnicastRemoteObject/exportObject impl 0)
        ;;_ (.println System/out (format "stub: %s" stub))
        rmi-reg (java.rmi.registry.LocateRegistry/createRegistry 1099)]
    (.rebind rmi-reg "RmiExample7.MyService" stub)
    (.println System/out "RMI server is waiting for incoming calls...")))

#_
(.bindRoot Compiler/LOADER (java.net.URL. "http://127.0.0.1:8080/class-server/"))
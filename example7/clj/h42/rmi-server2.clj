;;(defmacro foo [])

(defn run-rmi-server2 [& {:keys [host port ssf csf r-ssf r-csf] :or {port 1099}}]
  (let [cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
        hndlr (proxy [java.lang.reflect.InvocationHandler] []
                (invoke [proxy method args]
                  (.println System/out (str "Invoking method '" method "' with args '" args "'"))))
        prxy-clss (java.rmi.server.RMIClassLoader/loadProxyClass
                   nil 
                   (into-array ["RmiExample7$MyService"])
                   cl)
        impl (.newInstance (.getConstructor prxy-clss (into-array [java.lang.reflect.InvocationHandler]))
                           (into-array [hndlr]))
        stub (java.rmi.server.UnicastRemoteObject/exportObject impl 0 csf ssf)
        rmi-reg (if host
                  (do
                    (.println System/out (format "Connecting to RMI registry on host/port %s/%s with csf %s"
                                                 host port r-csf))
                    (java.rmi.registry.LocateRegistry/getRegistry host port r-csf))
                  (do
                    (.println System/out (format "Creating RMI registry on port %s with csf %s and ssf %s"
                                                 port r-csf r-ssf))
                    (java.rmi.registry.LocateRegistry/createRegistry port r-csf r-ssf)))]
    (.rebind rmi-reg "RmiExample7.MyService" stub)
    (.println System/out (format "Registered %s" stub))
    (.println System/out (format "Waiting for incoming calls on RMI %s / service %s" rmi-reg stub))))

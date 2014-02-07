(defn run-rmi-server [& {:keys [ssf csf r-ssf r-csf]}]
  (let [cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
        ifc (Class/forName "RmiExample7$MyService" false cl)
        hndlr (proxy [java.lang.reflect.InvocationHandler] []
                (invoke [proxy method args]
                  (.println System/out (str "Invoking method '" method "' with args '" args "'"))))
        impl (java.lang.reflect.Proxy/newProxyInstance cl (into-array [ifc]) hndlr)
        stub (java.rmi.server.UnicastRemoteObject/exportObject impl 0 csf ssf)
        rmi-reg (java.rmi.registry.LocateRegistry/createRegistry 1099 r-csf r-ssf)]
    (.rebind rmi-reg "RmiExample7.MyService" stub)
    (.println System/out (format "Registered %s" stub))
    (.println System/out (format "Waiting for incoming calls on %s" rmi-reg))))


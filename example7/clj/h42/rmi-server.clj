(defn run-rmi-reg [& {:keys [port r-ssf r-csf] :or {port 1099}}]
  (let [cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
        rmi-reg (let [ctcc (-> (Thread/currentThread) (.getContextClassLoader))]
                  (try
                    (-> (Thread/currentThread) (.setContextClassLoader cl))
                    (java.rmi.registry.LocateRegistry/createRegistry port r-csf r-ssf)
                    (finally
                      (-> (Thread/currentThread) (.setContextClassLoader ctcc)))))]
    (.println System/out (format "Waiting for incoming calls on %s" rmi-reg))))

(defn run-rmi-server [& {:keys [host port ssf csf r-ssf r-csf] :or {port 1099}}]
  (let [cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
        ifc (Class/forName "RmiExample7$MyService" false cl)
        hndlr (proxy [java.lang.reflect.InvocationHandler] []
                (invoke [proxy method args]
                  (.println System/out (str "Invoking method '" method "' with args '" args "'"))))
        impl (java.lang.reflect.Proxy/newProxyInstance cl (into-array [ifc]) hndlr)
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


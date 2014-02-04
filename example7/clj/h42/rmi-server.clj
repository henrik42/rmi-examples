#_ (to-inet-addr "localhost")
#_ (to-inet-addr "127.0.0.5")
#_ (to-inet-addr [127 0 0 1])

(defn to-inet-addr [addr]
  (cond
    (string? addr) (java.net.InetAddress/getByName addr)
    :else (java.net.InetAddress/getByAddress (into-array Byte/TYPE addr))))

#_ (.createServerSocket (server-socket-factory) 6666)
#_
(.createServerSocket (server-socket-factory :port 4444) 6666)

#_
(defn server-socket-factory [& {:keys [addr port backlog] :or {backlog 0}}]
  (let [addr (and addr (to-inet-addr addr))]
    (proxy [java.rmi.server.RMIServerSocketFactory] []
      (createServerSocket [p]
        (let [pp (or port p)]
          (try
            (.println System/out "foo")
            (java.net.ServerSocket. pp backlog addr)
            (catch Throwable t
              (do (.println System/out (format "oops %s" t))
              (throw (RuntimeException. (format "Failed for port %s (arg=%s this=%s" pp p this) t)))))))
      (toString [] (format "[server-socket-factory: addr=%s  port=%s  backlog=%s]"
                           addr port backlog))
      (equals [o]
        (.println System/out (format "equals?(%s %s)" this o))
        (= (class this) (class o))))))

(defn new-ssf [& {:keys [addr port backlog] :or {backlog 0}}]
  (let [addr (and addr (to-inet-addr addr))]
    (proxy [java.rmi.server.RMIServerSocketFactory] []
      (createServerSocket [p]
        (java.net.ServerSocket. (or port p) backlog addr))
      (toString [] (format "[server-socket-factory: addr=%s  port=%s  backlog=%s]"
                           addr port backlog))
      (equals [o]
        (.println System/out (format "equals?(%s %s)" this o))
        (= (class this) (class o))))))

#_
(defn new-server-socket-factory [& {:keys [local-port backlog local-addr addr] :or {backlog 0}}]
  (let [local-addr (when local-addr (to-inet-addr local-addr))]
    (proxy [java.rmi.server.RMIServerSocketFactory] []
      (createServerSocket [p] ;; create server socket (listening socket)
        (java.net.ServerSocket. (if local-port local-port p) backlog local-addr))
      (createSocket [h p] ;; create client socket 
        (java.net.Socket. addr port local-addr local-port))
      (toString [] (format "[server-socket-factory: port = %s  backlog = %s  local-addr = %s]"
                           port backlog local-addr))
      (equals [o]
        (.println System/out (format "equals?(%s %s)" this o))
        (= (class this) (class o))))))

#_
(def ssf (new-server-socket-factory :addr [127 0 0 1]))

(defn run-rmi-server [& {:keys [ssf]}]
  (let [cl (java.net.URLClassLoader.
            (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
        ifc (Class/forName "RmiExample7$MyService" false cl)
        hndlr (proxy [java.lang.reflect.InvocationHandler] []
                (invoke [proxy method args]
                  (.println System/out (str "Invoking method '" method "' with args '" args "'"))))
        impl (java.lang.reflect.Proxy/newProxyInstance cl (into-array [ifc]) hndlr)
        stub (java.rmi.server.UnicastRemoteObject/exportObject impl 0)
        rmi-reg (java.rmi.registry.LocateRegistry/createRegistry 1099 nil ssf)]
    (.rebind rmi-reg "RmiExample7.MyService" stub)
    (.println System/out (format "Waiting for incoming calls on RMI registry %s" rmi-reg))))


(defn to-inet-addr [addr]
  (cond
    (string? addr) (java.net.InetAddress/getByName addr)
    :else (java.net.InetAddress/getByAddress (into-array Byte/TYPE addr))))

(defn new-ssf [& {:keys [addr port backlog] :or {backlog 0}}]
  (let [addr (and addr (to-inet-addr addr))
        r (proxy [java.rmi.server.RMIServerSocketFactory] []
            (createServerSocket [p]
              (.println System/out (format "%s : opens server socket on port %s" this (or port p)))
              (java.net.ServerSocket. (or port p) backlog addr))
            (toString [] (format "[ssf: addr=%s  port=%s  backlog=%s]"
                                 addr port backlog))
            (equals [o]
              (let [r (= (class this) (class o))]
                (.println System/out (format "EQUALS! %s.equals(%s) --> %s" this o r))
                r)))]
    (.println System/out (format "NEW! %s" r))
    r))

(defn new-csf [& {:keys [addr port backlog] :or {backlog 0}}]
  (let [r (proxy [java.rmi.server.RMIClientSocketFactory java.io.Serializable] []
            (createSocket [h p]
              (.println System/out (format "%s : opens (client) socket on ip %s and port %s"
                                           this (to-inet-addr (or addr h)) (or port p)))
              (java.net.Socket. (to-inet-addr (or addr h)) (or port p)))
            (toString [] (format "[csf: addr=%s  port=%s  backlog=%s]" addr port backlog))
            (equals [o]
              (let [r (= (class this) (class o))]
                (.println System/out (format "EQUALS! %s.equals(%s) --> %s" this o r))
                r)))]
    (.println System/out (format "NEW! %s" r))
    r))


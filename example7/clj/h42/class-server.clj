;; A class-server
;;
;; Beispiel ohne Namespace. Dadurch kann man den Unterschied zwischen
;; einem 'echten' Skript und einer 'Lib' zeigen.
;;
;; (ns class-server)

;; TODO:
;; - handle-get-class und handle-get-jar/package um auch die Möglichkeit zu
;; schaffen, dass man Ressourcen mehrfach lädt über eine JAR Datei.
;; 
(defn- log [& xs]
  (.println System/out (apply str xs)))

(defn- consume-is [is & [os]]
  (let [os (or os (java.io.ByteArrayOutputStream.))
        bffr (byte-array 8192)]
    (loop []
      (let [r (.read is bffr)]
        (if (= -1 r) (when (instance? java.io.ByteArrayOutputStream os) (.toByteArray os))
            (do 
              (.write os bffr 0 r)
              (recur)))))))

(defn- resource-bytes [r]
  (let [cl (-> (Thread/currentThread) .getContextClassLoader)
        hits (enumeration-seq (.getResources cl r))]
    (when hits
      ;; (log "Resources for '" r "' : " hits)
      (with-open [is (.openStream (first hits))]
        (consume-is is)))))

(defn- handle-exchange [xchng pttrn]
  (try 
    (let [req-uri (str (.getRequestURI xchng))
          _ (log "Received request '" req-uri "'")
          _ (consume-is (.getRequestBody xchng)) 
          [_ cls-name] (or (re-matches pttrn req-uri)
                           (throw (RuntimeException.
                                   (str
                                    "Request "
                                    req-uri
                                    " does not match "
                                    pttrn))))
          cls-bytes (or
                     (resource-bytes cls-name)
                     (throw (RuntimeException. (str
                                                "Resource '"
                                                cls-name
                                                "' not found."))))
          _ (log (str "Returning " (alength cls-bytes) " bytes for resource '" cls-name "'"))]
      (.sendResponseHeaders xchng 200 (alength cls-bytes))
      (consume-is (java.io.ByteArrayInputStream. cls-bytes) (.getResponseBody xchng)))
    (catch Throwable t (log "handle-exchange failed !! : " t (with-out-str (.printStackTrace t (java.io.PrintWriter. *out*)))))
    (finally (.close xchng))))

(let [t (RuntimeException. "foo")
      s (with-out-str (.printStackTrace t (java.io.PrintWriter. *out*)))
      ]
  s)

(clojure.lang.Reflector/getMethods RuntimeException 1 "printStackTrace" false)

(defn run-class-server []
  (let [isa (java.net.InetSocketAddress. "127.0.0.1" 8080)
        pttrn #"/class-server/(.*)"
        _ (log "Starting HTTP class server on " isa " with pattern " pttrn)
        srvr (com.sun.net.httpserver.HttpServer/create isa 0)]
    (doto srvr
      (.createContext
       "/class-server/"
       (proxy [com.sun.net.httpserver.HttpHandler] []
         (handle [xchng]
           (handle-exchange xchng pttrn))))
      (.start))
    (log "Class server is waiting for incoming calls ...")))


#_ ;; curl http://127.0.0.1:8080/class-server/java/lang/String.class
(def srvr (run-class-server))

#_ 
(.stop srvr 0)


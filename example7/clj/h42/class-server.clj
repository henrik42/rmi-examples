(defn- log [& xs]
  (.println System/out (apply str xs)))

(def the-class-cache (atom {}))

(defn- get-cache [r]
  (let [e (@the-class-cache r)]
    (log (format "GET-CACHE: '%s' --> %s" r e))
    e))

(defn put-cache [class-name class-bytes]
  (let [k (str class-name ".class")]
    (log (format "PUT-CACHE: '%s'" k))
    (swap! the-class-cache assoc k class-bytes)))

(defn- resource-bytes [r]
  (let [cl (-> (Thread/currentThread) .getContextClassLoader)
        hits (enumeration-seq (.getResources cl r))]
    (when hits
      ;; (log "Resources for '" r "' : " hits)
      (with-open [is (.openStream (first hits))]
        (copy-io! is)))))

(defn- handle-exchange [xchng pttrn]
  (try 
    (let [req-uri (str (.getRequestURI xchng))
          _ (log "Received request '" req-uri "'")
          _ (copy-io! (.getRequestBody xchng)) ;; consume body!
          [_ cls-name] (or (re-matches pttrn req-uri)
                           (throw (RuntimeException.
                                   (str
                                    "Request "
                                    req-uri
                                    " does not match "
                                    pttrn))))
          cls-bytes (or
                     (resource-bytes cls-name)
                     (get-cache cls-name)
                     (throw (RuntimeException. (str
                                                "Resource '"
                                                cls-name
                                                "' not found."))))
          _ (log (str "Returning " (alength cls-bytes) " bytes for resource '" cls-name "'"))]
      (.sendResponseHeaders xchng 200 (alength cls-bytes))
      (copy-io! (java.io.ByteArrayInputStream. cls-bytes) (.getResponseBody xchng)))
    (catch Throwable t (log "handle-exchange failed !! : " t))
    (finally (.close xchng))))

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


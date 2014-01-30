;; Problemfall: der Compiler benötigt schon unseren RMI-ClassLoader
;; und den müssen wir aber erst setzten.
;; UND man muss ihn so setzen, dass der Compiler ihn nicht
;; durch seinen eigenen überschreibt.
;; Idee: wir binden Compiler/LOADER programmatisch
;; 

(def url-cl
  (let [r (java.net.URLClassLoader.
           (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
    (.println System/out (str "URL Classloader : " r))
    r))

(def dyn-cl
  (let [r (clojure.lang.DynamicClassLoader. url-cl)]
    r))

(defmacro compile-with-cl [body]
  (.addURL @Compiler/LOADER (java.net.URL. "http://127.0.0.1:8080/class-server/"))
  `(~@body))

#_
(defmacro compile-with-cl [& body]
  (.println System/out "(c1) push cl")
  (clojure.lang.Var/pushThreadBindings {Compiler/LOADER url-cl})
  (let [b (list 'let [] body)
        _ (.println System/out (str "(c2) compiling " b))
        ;;r# (clojure.lang.Compiler/eval b)
        r# (clojure.lang.Compiler/analyze clojure.lang.Compiler$C/EXPRESSION b)
        ]
    (clojure.lang.Var/popThreadBindings)
    (.println System/out (format "(c3) returning %s" r#))
    (compile r#)))
  
#_
(defmacro compile-with-cl [cl & body]
  (let [_ (.println System/out (format "compiling %s" cl))
        c (clojure.lang.Compiler/eval cl)
        _ (.println System/out (format "having %s" c))]
    (clojure.lang.Var/pushThreadBindings {Compiler/LOADER c})
    (try 
      (clojure.lang.Compiler/eval body)
      (finally 
        (clojure.lang.Var/popThreadBindings)))))

(let []
  (.println System/out "(m1) xx")
  (compile-with-cl 
    (.println System/out (str "(m2) foo: " foo.MyService))
    #_ (.println System/out "(m3) zz"))
  (.println System/out "(m4) AA")
  nil)


# Overview

This is a small collection of [RMI](link to tutorial) examples and an
integration with clojure. Eventhough RMI is an 'old technology' it's
still in use (e.g. EJB). There are many shortcommings and some even
say [it sucks](http://foo.bar/link).

See also http://nakkaya.com/2009/12/05/distributed-clojure-using-rmi/

# RmiExample1

Connect an RMI client to an RMI server without using a
registry. Instead the server writes the serialized RMI stub to a file
from which the client reads it.

**TODO**: der Name der Datei sollte als Kommandozeilenargument
  angegeben werden.

+ Compile

		example1$ rm -rf bin/*
		example1$ javac -d bin/ src/RmiExample1.java
	
+ Run the server in one shell and the client in a second shell.
  
		example1$ java -cp bin/ 'RmiExample1$Server'
		example1$ java -cp bin/ 'RmiExample1$Client'
		
# RmiExample2

An RMI server that **creates and executes** the RMI registry in the
server's JVM. The client may run in the same JVM or in a seperate one.

+ Compile

		example2$ rm -rf bin/*
		example2$ javac -d bin/ src/RmiExample2.java
	
+ Run Server incl. RMI registry in one shell and the client in a second shell.
  
		example2$ java -cp bin/ 'RmiExample2$Server'
		example2$ java -cp bin/ 'RmiExample2$Client'

# RmiRegistry (example3)

A simple RMI registry. You could use ```rmiregistry``` instead.

+ Compile

		example3$ rm -rf bin/*
		example3$ javac -d bin/ src/RmiRegistry.java
	
+ Run the RMI registry
  
		example3$ java -cp bin/ RmiRegistry

## Using the RmiRegistry

Now you can run the ```RmiExample2``` server with **```connect```** against
the ```RmiRegistry```:

	example2$ java -cp bin/ 'RmiExample2$Server' connect

But you'll get something like this:

	Exception in thread "main" java.rmi.ServerException: RemoteException occurred in server thread; nested exception is: 
		java.rmi.UnmarshalException: error unmarshalling arguments; nested exception is: 
		java.lang.ClassNotFoundException: RmiExample2$MyService (no security manager: RMI class loader disabled)
		at sun.rmi.server.UnicastServerRef.oldDispatch(UnicastServerRef.java:419)
		[....]
		at RmiExample2$Server.main(RmiExample2.java:54)
	Caused by: java.rmi.UnmarshalException: error unmarshalling arguments; nested exception is: 
		java.lang.ClassNotFoundException: RmiExample2$MyService (no security manager: RMI class loader disabled)
		at sun.rmi.registry.RegistryImpl_Skel.dispatch(Unknown Source)
		[....]
		at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
		at java.lang.Thread.run(Thread.java:724)
	Caused by: java.lang.ClassNotFoundException: RmiExample2$MyService (no security manager: RMI class loader disabled)
		at sun.rmi.server.LoaderHandler.loadProxyClass(LoaderHandler.java:555)
		at java.rmi.server.RMIClassLoader$2.loadProxyClass(RMIClassLoader.java:646)
		at java.rmi.server.RMIClassLoader.loadProxyClass(RMIClassLoader.java:311)
		[....]
		at java.io.ObjectInputStream.readObject(ObjectInputStream.java:370)
		... 13 more

The ```Registry``` deserializes the stub and thus needs the class
definition for the interface ```RmiExample2$MyService```. Since the
classpath did not contain the class we got a
```ClassNotFoundException: RmiExample2$MyService```.

Start the ```RmiRegistry``` with the classpath containing
```../example2/bin/```:

		example3$ java -cp bin/:../example2/bin/ RmiRegistry

And now:

		example2$ java -cp bin/ 'RmiExample2$Server' connect

And:

		example2$ java -cp bin/ 'RmiExample2$Client'

# RmiExample4

This example **will not work**. I just kept it for demonstration. In
```RmiExample1``` and ```RmiExample2``` the server and the client
**exchange** a serialized object (stub) for the remote call.

In this example the client creates the stub *locally* (without
exchanging anything) and uses an ```RMIClientSocketFactory``` to
connect to the remote server.

+ Compile

		example4$ rm -rf bin/*
		example4$ javac -d bin/ src/RmiExample4.java

+ Run

		example4$ java -cp bin/ 'RmiExample4$Server'
		example4$ java -cp bin/ 'RmiExample4$Client'

	But this will give you:

		Exception in thread "main" java.rmi.NoSuchObjectException: no such object in table
			at sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:273)
			at sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:251)
			at sun.rmi.server.UnicastRef.invoke(UnicastRef.java:160)
			at java.rmi.server.RemoteObjectInvocationHandler.invokeRemoteMethod(RemoteObjectInvocationHandler.java:194)
			at java.rmi.server.RemoteObjectInvocationHandler.invoke(RemoteObjectInvocationHandler.java:148)
			at $Proxy0.voidMethod(Unknown Source)
			at RmiExample4$Client.main(RmiExample4.java:73)

	The call does indeed make it to the server but the server's RMI
	runtime detects, that there is no *matching target* for the
	incomming call (see
	```sun.rmi.transport.ObjectTable.getTarget(ObjectEndpoint)```). So
	the call fails. I haven't done any more experiments and tried to
	somehow make the RMI runtime accept the call. So this is a dead
	end for now.

# RmiExample5

In ```RmiExample3``` we found that the ```RmiRegistry``` deserializes
the stub object and thus needs the class definition for the
interface. This is also true for the client when it retrieves the stub
from the registry - the client too needs access to the class
definition.

```RmiExample5``` is a **generic** RMI client that has **no class
definition for the server's stub classes and interfaces**.

It will retrieve the servers stubs (which contains the instance data
only **but not the class definiton**!) via a registry and will use an
```URLClassLoader``` to **dynamically** load the class definitions
(via HTTP) that are needed for deserialization (**on demand**). This
is even true for method parameter and return types and for exception
types also!  So there does not need to be **any upfront exchange of
class files**.

In ```RmiExample6``` we will implement a simple *class server* (that
runs in the RMI server's JVM) that gives access to the server's class
definitions via HTTP.

This *zero deployment setup* comes in handy when you deploy EJB
applications and you have remote clients that want to connect to the
EJB beans via RMI (you do have to deploy the *class-server*
though). In such situations you usually have to supply (*deploy*)
container specific classes (JARs) (e.g. 'JBoss client JARs') and
sometimes even EJB specific classes that you do not have at build time
because they are built during deplyoment (IBM Websphere).

+ Compile

		example5$ rm -rf bin/*
		example5$ javac -d bin/ src/RmiExample5.java
		
+ Run (start the ```RmiExample6$Server``` before this)

		example5$ java -cp bin/ 'RmiExample5$Client'		

# RmiExample6

An RMI server (with RMI registry) incl. a class server that serves
class definitions (which are visible through it's own classpath) via
HTTP-GET.

This class server is based on ```com.sun.net.httpserver.HttpServer```.

+ Compile

		example6$ rm -rf bin/*
		example6$ javac -d bin/ src/RmiExample6.java

+ Run

		example6$ java -cp bin/ 'RmiExample6$Server'

Now you can use the ```RmiExample5$Client``` from above to connect to
this RMI server.

# Using clojure

The following examples show how the RMI client/server and the class
server can be implemented in clojure.
	
## RmiExample7

+ Run ```class-server```

	The code in ```class-server.clj``` has no **name space** and is
    not ment to be ```use```d as a *library*. Instead it is just
    loaded (and compiled) as a script (into ```user``` namespace).

	When I started out with clojure it took me a while to understand
    how to use clojure source files as a *true script* (which is just
    loaded via plain file IO) and when to ```use``` it as a re-useable
    library (usually via classloading/classpath -- which is file IO in
    the end too though ```;-)``` ).

	In order to keep the code short and to not put things in that
	aren't relevant for what I'm tring to do, I decided to not use any
	namespace in this example. In *real code* you **will** use
	namespaces though and you will break up the code into many more
	functions.

	Note that the **classpath includes ```bin/```**. Otherwise the
    server (as it is implemented now) could not find the class
    definition files.

	All we need to compile in this example is the ```Remote```
	interface. We do **not have any implementation in Java in this
	example!**.
	
		example7$ rm -rf bin/*
		example7$ javac -d bin/ src/RmiExample7.java

	Now we *serve* the ```Remote``` interface class definition (and
    other classes as well):
	
		example7$ java -cp lib/clojure.jar:bin/ clojure.main -i clj/h42/class-server.clj -e '(run-class-server)'
				
+ Run ```rmi-server``` with registry

	This is it:

		(defn run-rmi-server []
		  (let [cl (java.net.URLClassLoader.
					(into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
				ifc (Class/forName "RmiExample7$MyService" false cl)
				hndlr (proxy [java.lang.reflect.InvocationHandler] []
						(invoke [proxy method args]
						  (.println System/out (str "Invoking method '" method "' with args '" args "'"))))
				impl (java.lang.reflect.Proxy/newProxyInstance cl (into-array [ifc]) hndlr)
				stub (java.rmi.server.UnicastRemoteObject/exportObject impl 0)
				rmi-reg (java.rmi.registry.LocateRegistry/createRegistry 1099)]
			(.rebind rmi-reg "RmiExample7.MyService" stub)
			(.println System/out "RMI server is waiting for incoming calls...")))
  
  The ```rmi-server```

	+ creates a classloader which points to the ```class-server```'s
      HTTP service
	+ **loads the interface via reflection** (see below)
	+ creates a *dynamic proxy* for the ```Remote``` interface 
	+ publishes this object (which gives the stub)
	+ starts the RMI registry
	+ and binds the stub

			example7$ java -cp lib/clojure.jar clojure.main -i clj/h42/rmi-server.clj -e '(run-rmi-server)'

	Note that the classpath does **not include ```bin/```**. If it did
	the server would load the class definitions via file IO and not
	via ```HTTP-GET``` from the ```class-server``` due to the *parent
	first delegation strategy* in
	```java.lang.ClassLoader.loadClass(String, boolean)``` (go ahead
	and try it).

	**Note:** There seem to be cases when this call returns ---
	i.e. the JVM exits --- right after ```run-rmi-server``` has
	returned. This is probably because there are only *daemon threads*
	running. In this case the JVM will terminate. But there are other
	times when this does not happen. I haven't done any research on
	this matter. Instead I just use an extra ```-e '@(promise)'``` at
	the end of the command line to keep the JVM running. So the above
	call becomes:

			example7$ java -cp lib/clojure.jar clojure.main -i clj/h42/rmi-server.clj -e '(run-rmi-server)' -e '@(promise)'

	**Note:** In the ```rmi-server``` I used

			(java.rmi.server.UnicastRemoteObject/exportObject impl 0)

	and not just

			(java.rmi.server.UnicastRemoteObject/exportObject impl)
	
	If you run the second variant you will get

			Exception in thread "main" java.lang.RuntimeException: java.rmi.StubNotFoundException: Stub class not found: com.sun.proxy.$Proxy0_Stub; nested exception is: 
				java.lang.ClassNotFoundException: com.sun.proxy.$Proxy0_Stub
				at clojure.lang.Util.runtimeException(Util.java:165)
				at clojure.lang.Compiler.eval(Compiler.java:6476)
				at clojure.lang.Compiler.eval(Compiler.java:6431)
				at clojure.core$eval.invoke(core.clj:2795)
				at clojure.main$eval_opt.invoke(main.clj:296)
				at clojure.main$initialize.invoke(main.clj:315)
				at clojure.main$null_opt.invoke(main.clj:348)
				at clojure.main$main.doInvoke(main.clj:426)
				at clojure.lang.RestFn.invoke(RestFn.java:512)
				at clojure.lang.Var.invoke(Var.java:421)
				at clojure.lang.AFn.applyToHelper(AFn.java:185)
				at clojure.lang.Var.applyTo(Var.java:518)
				at clojure.main.main(main.java:37)
			Caused by: java.rmi.StubNotFoundException: Stub class not found: com.sun.proxy.$Proxy0_Stub; nested exception is: 
				java.lang.ClassNotFoundException: com.sun.proxy.$Proxy0_Stub
				at sun.rmi.server.Util.createStub(Util.java:292)
				at sun.rmi.server.Util.createProxy(Util.java:140)
				at sun.rmi.server.UnicastServerRef.exportObject(UnicastServerRef.java:196)
				at java.rmi.server.UnicastRemoteObject.exportObject(UnicastRemoteObject.java:310)
				at java.rmi.server.UnicastRemoteObject.exportObject(UnicastRemoteObject.java:237)
				at user$run_rmi_server.invoke(rmi-server.clj:9)
				at user$eval5.invoke(NO_SOURCE_FILE:1)
				at clojure.lang.Compiler.eval(Compiler.java:6465)
				... 11 more
			Caused by: java.lang.ClassNotFoundException: com.sun.proxy.$Proxy0_Stub
				at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
				at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
				at java.security.AccessController.doPrivileged(Native Method)
				at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
				at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
				at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
				at java.lang.Class.forName0(Native Method)
				at java.lang.Class.forName(Class.java:270)
				at sun.rmi.server.Util.createStub(Util.java:286)
				... 18 more

	The reason for this seems to be that
	```java.rmi.server.UnicastRemoteObject.exportObject(Remote)```
	uses ```sun.rmi.server.UnicastServerRef.UnicastServerRef(true)```
	(see the Java doc for the constructur for more details). I did
	some google-ing and found this:

	+ http://www.stratos.me/2008/05/stub-class-not-found/
	
	+ http://stackoverflow.com/questions/10648026/why-the-class-cannot-be-seen-in-its-source-file-java
	
	+ http://osdir.com/ml/java.sun.rmi/2006-10/msg00000.html

	I still do not understand why there is a class
	```com.sun.proxy.$Proxy0_Stub``` that is generated and loaded(?)
	but cannot be accessed afterwards. Anyway ...

+ Now run ```rmi-client```

	This is it:

		(defn run-rmi-client []
		  (let [rmi-reg (java.rmi.registry.LocateRegistry/getRegistry 1099)
				cl (java.net.URLClassLoader.
					(into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
			(.setContextClassLoader (Thread/currentThread) cl)
			(.voidMethod (.lookup rmi-reg "RmiExample7.MyService"))))

	(again the classpath does not contain ```bin/```)
	
		example7$ java -cp lib/clojure.jar clojure.main -i clj/h42/rmi-client.clj -e '(run-rmi-client)'

## Using the Java inter-op

The ```run-rmi-server``` from above uses reflection
(```Class/forName``` und
```java.lang.reflect.Proxy/newProxyInstance```) and not clojure's Java
inter-op.

Now let's try (see ```rmi-server2.clj```) using the ```proxy``` macro
instead. In this case you **must** use the class literals and cannot
use reflection -- like this:

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

(Note: we **have to** use a class -- ```foo.MyService``` in this case
-- which is **not from the default package**)

We use ```(.setContextClassLoader (Thread/currentThread) cl)``` so
that clojure will use it to load ```foo.MyService```.

And run it like:

	example7$ java -cp lib/clojure.jar clojure.main -i clj/h42/rmi-server2.clj -e '(run-rmi-server2)'

**It wont' work!** It will give you a
```java.lang.ClassNotFoundException: foo.MyService```. The reason is,
that the **compiler** tries to load the class ```foo.MyService``` at
**compile** time (i.e. macro expansion) before the function is
executed (at **eval** time).

But there is a simple trick: **set the classloader before the form is
compiled**. You could do this by first running ```set-ctccl.clj```
(you may remove the call to ```.setContextClassLoader``` from
```run-rmi-server2``` if you like):

	example7$ java -cp lib/clojure.jar clojure.main -i clj/h42/set-ctccl.clj -i clj/h42/rmi-server2.clj -e '(run-rmi-server2)'

But this is not a nice solution. For many use cases the usage and
effect of our *remote classloader* should be encapsulated **inside**
the ```run-rmi-server2``` function -- and not affect anything that
will happen in the current thread.

The tricky thing about injecting our classloader into clojure's
compile-step comes from the way ```clojure.lang.Compiler.eval(Object,
boolean)``` works. It will use/create a *fresh* classloader each time
it is called and use ```clojure.lang.RT.makeClassLoader()``` to get
it's parent/delegate.

So even if we used ```with-bindings``` to bind
```clojure.lang.Compiler/LOADER``` to our classloader within a macro
it would be popped off the stack of the thread-local
```clojure.lang.Compiler/LOADER``` var-bindings before the time when
the class literal (i.e. ```symbol```) ```foo.MyService``` is
resolved/compiled.

I tried using ```with-bindings``` and
```clojure.lang.Var/pushThreadBindings``` and
```clojure.lang.Var/popThreadBindings``` within a macro in many
different ways but couldn't make it work.

The best solution I found is this (```rmi-class-server3.clj```):

	(defmacro compile-with-cl [body]
	  (.addURL @Compiler/LOADER (java.net.URL. "http://127.0.0.1:8080/class-server/"))
	  `(~@body))

	(defn run-rmi-server3 []
	  (let [impl (compile-with-cl
				   (proxy [foo.MyService] []
					 (voidMethod []
					   (.println System/out "executing voidMethod"))))
			stub (java.rmi.server.UnicastRemoteObject/exportObject impl 0)
			rmi-reg (java.rmi.registry.LocateRegistry/createRegistry 1099)]
		(.rebind rmi-reg "RmiExample7.MyService" stub)
		(.println System/out "RMI server is waiting for incoming calls...")))

And run it:

	example7$ java -cp lib/clojure.jar clojure.main -i clj/h42/rmi-server3.clj -e '(run-rmi-server3)' -e '@(promise)'

The ```compile-with-cl``` macro **changes** the current top
thread-local binding of ```Compiler/LOADER``` (which is a
```clojure.lang.DynamicClassLoader``` extending
```java.net.URLClassLoader```) at compile-time. Then before a new
binding of ```Compiler/LOADER``` is established the ```body```
containing the ```foo.MyService``` is compiled.

So this solution depends on **mutation** which I dislike. But in this
case I thinks it's ok: the *changed classloader* will be popped off
the stack right after the form has been compiled and as far as I can
tell it will be discarded and gc'ed. So it can't do any harm after
that. This should even be true if the compile threw an exception.

## Setting Compiler/LOADER's root binding

I still use Swank/Emacs/SLIME when writing clojure code. So when I
write the clojure code that will use the ```compile-with-cl``` macro I
want to auto-complete class-symbols and use SLIME-mode's
```slime-interactive-eval``` and enter the class-symbol into the
minibuffer and have Swank eval that. In this case
```compile-with-cl``` is not involved and I get a
```java.lang.ClassNotFoundException: foo.MyService```.

(let [url-cl (java.net.URLClassLoader.
			  (into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))
	  dyn-cl (clojure.lang.DynamicClassLoader. url-cl)]
  (.bindRoot Compiler/LOADER dyn-cl))

To fix this I'll do



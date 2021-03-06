# Overview

This is a collection of code examples in Java and Clojure that I put
together. After I had played around with RMI and tried different
things I thought it was time for my very first github project. So this
is it.

For a quick RMI/Clojure intro see
http://nakkaya.com/2009/12/05/distributed-clojure-using-rmi/

I ran all my examples on a Samsung Chromebook with an Ubuntu 12.04
LTS.

	Linux version 3.4.0 (chrome-bot@build63-m2) (gcc version 4.7.x-google 20130114 \
	  (prerelease) (4.7.2_cos_gg_c8f69e0) ) #1 SMP Wed Oct 23 03:22:56 PDT 2013
	CPU: ARMv7 Processor [410fc0f4] revision 4 (ARMv7), cr=10c5387d
	CPU: PIPT / VIPT nonaliasing data cache, PIPT instruction cache
	Machine: SAMSUNG EXYNOS5 (Flattened Device Tree), model: Google Snow

For Java I use OpenJDK 

	java version "1.7.0_25"
	OpenJDK Runtime Environment (IcedTea 2.3.10) (7u25-2.3.10-1ubuntu0.12.04.2)
	OpenJDK Zero VM (build 22.0-b10, mixed mode)

and

	java version "1.8.0-ea"
	Java(TM) SE Runtime Environment (build 1.8.0-ea-b116)
	Java HotSpot(TM) Client VM (build 25.0-b58, mixed mode)

And for Clojure I used an old clojure 1.3.0. I'll do tests
against 1.5.x in the future.

# RmiExample1

Connect an RMI client to an RMI server **without using a
registry**. Instead the server writes the serialized RMI stub to a
file from which the client reads it.

This example is just for playing around. I wanted to see how *tightly
coupled* the client and the server had to be and how one may get away
without using the registry.

You can set ```RmiExample1.MyService.PORT``` to non-zero in order to
run the server on a fixed port number. See ```RmiExample2``` for a
more realistic use case.

+ Compile

		example1$ rm -rf bin/*
		example1$ javac -d bin/ src/RmiExample1.java
	
+ Run the server in one shell and the client in a second shell.
  
		example1$ java -cp bin/ 'RmiExample1$Server'
		example1$ java -cp bin/ 'RmiExample1$Client'

## Some thoughts about RMI

I really dislike that RMI forces me to use RMI-specific
classes/interfaces (like ```extends Remote``` and ```throws
RemoteException```) for the *service contract*
(```RmiExample1.MyService``` in this case). I cannot just use any
*plain Java interface* and tell RMI to make a remote call. The *RMI
API is infective*.

**TODO: you can use a generic proxy --- like the one from Spring)**

Of course there is more to this: if we could use just any Java
interface method to make a remote call there would be no way to say,
for example that the returned value should be a *remote object*. In
this case you only have a *handle* to the object (which may live in
another JVM) and not the (copy of the) object itself. With RMI you say
just this by having the returned class extend
```java.rmi.Remote```. So the idea of making RMI calls *against* any
Java interface with by-value semantics would just give us a *remote
procedure call*-like (RPC) solution. RMI gives us a *distributed
object* solution. For many use cases though the RPC solution is all
you want.

**TODO: RMI and co-references**

**TODO: RMI and equals for remote objects.**

# RmiExample2

An RMI server that **creates and runs** the RMI registry in the
server's JVM (below you find an example of running the RMI registry in
it's own process/JVM). The client may run in the same JVM or in a
seperate one.

+ Compile

		example2$ rm -rf bin/*
		example2$ javac -d bin/ src/RmiExample2.java
	
+ Run Server incl. RMI registry in one shell and the client in a second shell.
  
		example2$ java -cp bin/ 'RmiExample2$Server'
		example2$ java -cp bin/ 'RmiExample2$Client'

## Calling the server through a firewall

Again you can set ```RmiExample2.MyService.PORT``` to non-zero in
order to run the server on a fixed port number. You can use this if
you want to call the server through a firewall.

In this case you have to open **two ports** in the firewall: one for
the RMI registry and another for the service/object you want to
call.

## Telling the client how to connect

Note that the server puts **connection data** for the client into the
object that it registers with the RMI registry (from where the client
retrieves it). Often this is a source of problems since the client
uses these to connect to the service/remote object --- **not to the
registry**!! So if the registered object contained
```localhost:1234``` and the client ran on a different host, it would
fail to connect.

There are other scenarios:

+ Client and server are located in different *domains* and the client
  receives ```somehost:1234```. If the clients DNS server cannot
  resolve the hostname (because it has a different default domain),
  the clients fails.

+ When the server has more than one TCP/IP interface (*multi-homed
  host*) and only one is reachable from the client (i.e. there is a
  TCP/IP route to only one if the server's interfaces/IP addresses)
  then there is a chance that the server delivers the wrong IP or
  hostname to the client. For the hostname it depends on how the
  client's DNS server resolves it.

See what the server prints on startup: 

	exporting RmiExample2$MyServiceRmiImpl@13974ba on port 0
	Starting RMI registry on port 1099
	Binding Proxy[RmiExample2$MyService,RemoteObjectInvocationHandler[UnicastRef [liveRef: [endpoint:[127.0.0.1:40487] \
	  (local),objID:[-d39bdd5:143e9a66f67:-7fff, -918886190803948582]]]]] with id RmiExample2.MyService to RMI registry

And this:

	example2$ netstat -na | egrep '1099|40487'
	tcp6       0      0 :::40487                :::*                    LISTEN     
	tcp6       0      0 :::1099                 :::*                    LISTEN     

So the server tells the client to connect to the remote object on
```127.0.0.1:40487```. If the client ran on a remote host this would
not work!

Although the server uses ```127.0.0.1:40487``` for the client
connection data it opens it's own ```ServerSocket``` on
```0.0.0.0:40487``` (and not ```127.0.0.1:40487```). This may be what
you want or not. Opening a listening socket on ```127.0.0.1``` and not
on ```0.0.0.0``` is a simple way to keep remote clients from
connecting to your RMI services.

All these details are controlled by the
```java.rmi.server.RMIServerSocketFactory``` and the
```java.rmi.server.RMIClientSocketFactory```. See RmiExample7 below
for more about this.

Note that the connect info does not even have to include an existing
host! Try this:

	example2$ java -Djava.rmi.server.hostname=foo.bar -cp bin/ 'RmiExample2$Server'
	exporting RmiExample2$MyServiceRmiImpl@13974ba on port 0
	Starting RMI registry on port 1099
	Binding Proxy[RmiExample2$MyService,RemoteObjectInvocationHandler[UnicastRef [liveRef: [endpoint:[foo.bar:51564] \
	  (local),objID:[-6e293192:143e9ac855a:-7fff, 3177075534561695956]]]]] with id RmiExample2.MyService to RMI registry

With ```-Djava.rmi.server.hostname=foo.bar``` we tell the RMI runtime
which host info to put into the connection data. The server does not
even try to resolve the hostname! That's fine and correct: the connect
info is for the **client** and the **client** actually may be able to
resolve ```foo.bar```.

But mine isn't:

	example2$ java -cp bin/ 'RmiExample2$Client'
	Connecting to RMI registry on port 1099
	Looking up service of type MyService with id RmiExample2.MyService
	Calling voidMethod() on Proxy[RmiExample2$MyService,RemoteObjectInvocationHandler[UnicastRef [liveRef: [endpoint:[foo.bar:51564] \
	  (remote),objID:[-6e293192:143e9ac855a:-7fff, 3177075534561695956]]]]]
    Exception in thread "main" java.rmi.UnknownHostException: Unknown host: foo.bar; nested exception is:
        java.net.UnknownHostException: foo.bar

# RmiRegistry (example3)

A simple RMI registry. You could use ```rmiregistry``` instead.

+ Compile

		example3$ rm -rf bin/*
		example3$ javac -d bin/ src/RmiRegistry.java
	
+ Run the RMI registry
  
		example3$ java -cp bin/ RmiRegistry

## Using the RmiRegistry

Now you can run the ```RmiExample2``` server with ```connect```
against the ```RmiRegistry```:

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

Start the ```RmiRegistry``` with the classpath containing ```../example2/bin/```:

	example3$ java -cp bin/:../example2/bin/ RmiRegistry

And now:
	
	example2$ java -cp bin/ 'RmiExample2$Server' connect

And:

	example2$ java -cp bin/ 'RmiExample2$Client'

# RmiExample4

This example **will not work**. I just kept it for demonstration. In
```RmiExample1``` and ```RmiExample2``` the server and the client
**exchange** a serialized object (stub) for the remote call. Can we
get away **without exchanging anything**?

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
definition for the server's stub classes and interfaces at compile
time**.

At runtime it will retrieve the servers stubs (which contains the
instance data only **but not the class definiton**!) via the RMI
registry and will use an ```URLClassLoader``` to **dynamically** load
the class definitions (via HTTP from a *class server*) that are needed
for deserialization (**on demand**). This is even true for method
parameter and return types and for exception types also!  So there
does not need to be **any upfront exchange of class files**.

In ```RmiExample6``` we will implement a simple *class server* (that
runs in the RMI server's JVM) that gives access to the server's class
definitions via HTTP.

This *zero deployment setup* comes in handy when you deploy EJB
applications and you have remote clients that want to connect to the
EJB beans via RMI (you do have to deploy the *class-server*
though). In such situations you usually have to supply (*deploy*)
container specific classes (JARs) (e.g. 'JBoss client JARs') and
sometimes even EJB specific classes that you do not have at build time
because they are built during deployment (IBM Websphere).

**TODO: examples for JBoss and Websphere**

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

### The class-server

The code in ```class-server.clj``` has no **name space** and is not
ment to be ```use```d as a *library*. Instead it is just loaded (and
compiled) as a script (into ```user``` namespace).

When I started out with clojure it took me a while to understand how
to use clojure source files as a *true script* (which is just loaded
via plain file IO) and when to ```use``` it as a re-useable library
(usually via classloading/classpath -- which is file IO in the end too
though ```;-)``` ).

In order to keep the code short and to not put things in that aren't
relevant for what I'm tring to do, I decided to not use any namespace
in this example. In *real code* you **will** use namespaces though and
you will break up the code into many more functions.

Note that the **classpath includes ```bin/```**. Otherwise the
```class-server``` (as it is implemented now) could not find the class
definition files.

All we need to compile in this example is the ```Remote``` interface
```RmiExample7$MyService```. We do **not have any implementation in
Java in this example!**
	
	example7$ rm -rf bin/* && javac -d bin/ src/RmiExample7.java

Now we *serve* the ```Remote``` interface class definition (and other
classes as well):

	example7$ java -cp lib/clojure.jar:bin/ clojure.main \
	  -i clj/h42/misc.clj \
	  -i clj/h42/class-server.clj \
	  -e '(run-class-server)'

### The rmi-server

This is it (see ```rmi-server.clj```):

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
  
The ```rmi-server```

+ creates a classloader which points to the ```class-server```'s HTTP
  service
+ **loads the interface via reflection** (see below for why this is
  so)
+ creates a *Java dynamic proxy* for the ```Remote``` interface
+ publishes this object (which gives the stub)
+ starts the RMI registry or connects to a running registry
+ and binds the stub

Make sure that the ```class-server``` is running when running ```run-rmi-server```:
  
	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-server.clj \
	  -e '(run-rmi-server)'

The function takes some optional arguments which will be explained
later.

Note that the classpath does **not include ```bin/```**. If it did the
server would load the class definitions via file IO and not via
```HTTP-GET``` from the ```class-server``` due to the *parent first
delegation strategy* in ```java.lang.ClassLoader.loadClass(String,
boolean)``` (go ahead and try it).

There are cases (**when using the OpenJDK**) when this call returns --
i.e. the JVM exits -- right after ```(run-rmi-server)``` has
returned. This is probably because there are only *daemon threads*
running. In this case the JVM will terminate. But there are other
times when this does not happen. I haven't done any research on this
matter. Instead I just use an extra ```-e '@(promise)'``` at the end
of the command line to keep the JVM running. So the above call
becomes:

	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-server.clj \
	  -e '(run-rmi-server)' \
	  -e '@(promise)'

### The rmi-client

This is it (see ```rmi-client.clj```):

	(defn run-rmi-client [& {:keys [r-csf host port] :or {host "127.0.0.1" port 1099}}]
	  (let [rmi-reg (java.rmi.registry.LocateRegistry/getRegistry host port r-csf)
			_ (.println System/out (format "Using RMI registry %s" rmi-reg))
			cl (java.net.URLClassLoader.
				(into-array [(java.net.URL. "http://127.0.0.1:8080/class-server/")]))]
		(.setContextClassLoader (Thread/currentThread) cl)
		(let [stub (.lookup rmi-reg "RmiExample7.MyService")]
		  (.println System/out (format "Calling .voidMethod on %s" stub))
		  (.voidMethod stub))))

Now run the client (again the classpath does not contain ```bin/```):
	
	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -e '(run-rmi-client)'

### Controlling the client and server side sockets (RMI registry)

In ```sf.clj``` you find functions for creating ```java.rmi.server.RMIClientSocketFactory``` and ```java.rmi.server.RMIServerSocketFactory```.

You can use them to control the server-side sockets (*listening
sockets*) for the RMI registry and the registered ```Remote```
objects. And you can use them to control how the RMI client connects
to these server-side sockets.

First we tell the ```rmi-server``` to open it's RMI registry on
```127.0.0.2:1099``` instead of ```0.0.0.0:1099``` (make sure you have
the ```class-server``` running).

	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-server.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-server :r-ssf (new-ssf :addr "127.0.0.2"))' \
	  -e '@(promise)'

Check with:

	example7$ netstat -na | egrep '^tcp.*LISTEN'

Now run the client:

	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -e '(run-rmi-client :host "127.0.0.2")'

On the client side you'll get:

	[...]
	Caused by: java.rmi.NoSuchObjectException: no such object in table
        at sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:276)
        at sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:253)
        at sun.rmi.server.UnicastRef.invoke(UnicastRef.java:162)
        at java.rmi.server.RemoteObjectInvocationHandler.invokeRemoteMethod(RemoteObjectInvocationHandler.java:194)
        at java.rmi.server.RemoteObjectInvocationHandler.invoke(RemoteObjectInvocationHandler.java:148)
        at com.sun.proxy.$Proxy0.voidMethod(Unknown Source)
	[...]

I haven't really found the cause of this. There are two ways to fix
this.

The first is to tell Clojure to write its classes to the file-system
(for more on *generated classes* see below). Try this:

	example7$ mkdir -p ./class-cache/ \
	  && rm -rf ./class-cache/* \
	  && java -Dclojure.compile.path=./class-cache/ \
	    -cp lib/clojure.jar clojure.main \
	    -e '(.bindRoot Compiler/COMPILE_FILES true)' \
	    -i clj/h42/rmi-server.clj \
	    -i clj/h42/sf.clj \
	    -e '(run-rmi-server :r-ssf (new-ssf :addr "127.0.0.2"))' \
	    -e '@(promise)'

Here we make Clojure write the generated class-definitions to
```./class-cache/```. I do not use the default ```./classes/``` to be
sure that the ```rmi-client``` will not see them (although I haven't
checked that ```./classes/``` is indeed visible to any classloader by
default --- **TODO: check this**). Note that ```-e '(.bindRoot
Compiler/COMPILE_PATH "./class-cache/")'``` will **not work** because
this root binding is hidden by the ```clojure.main/with-bindings```
macro which is in effect when the code is run.

Eventhough the classpath does not include ```./classes/``` it still
seems to change *something* about the environment (which probably has
something to do with classloading). You should be able to run
```run-rmi-client``` with no problem.

The second fix seems to be a *real fix*: it uses the RMI API
```java.rmi.server.RMIClassLoader/loadProxyClass``` to create the
proxy instead of using
```java.lang.reflect.Proxy/newProxyInstance```. This is the code (see ```rmi-server2.clj```):

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

Now run this:

	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-server2.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-server2 :r-ssf (new-ssf :addr "127.0.0.2"))' \
	  -e '@(promise)'

Again you should be able to run the client with no problem.

### A note about java.rmi.server.UnicastRemoteObject/exportObject

While putting together the code I tried 

	stub (java.rmi.server.UnicastRemoteObject/exportObject impl)

instead of

	stub (java.rmi.server.UnicastRemoteObject/exportObject impl 0 csf ssf)

in ```run-rmi-server2``` -- but when you run that you'll get:

	Exception in thread "main" java.lang.RuntimeException: java.rmi.StubNotFoundException: \
	  Stub class not found: com.sun.proxy.$Proxy0_Stub; nested exception is: 
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
		at sun.rmi.server.Util.createStub(Util.java:297)
		at sun.rmi.server.Util.createProxy(Util.java:142)
		at sun.rmi.server.UnicastServerRef.exportObject(UnicastServerRef.java:197)
		at java.rmi.server.UnicastRemoteObject.exportObject(UnicastRemoteObject.java:383)
		at java.rmi.server.UnicastRemoteObject.exportObject(UnicastRemoteObject.java:301)
		at user$run_rmi_server2.doInvoke(rmi-server2.clj:14)
		at clojure.lang.RestFn.invoke(RestFn.java:397)
		at user$eval35.invoke(NO_SOURCE_FILE:1)
		at clojure.lang.Compiler.eval(Compiler.java:6465)
		... 11 more
	Caused by: java.lang.ClassNotFoundException: com.sun.proxy.$Proxy0_Stub
		at java.net.URLClassLoader$1.run(URLClassLoader.java:372)
		at java.net.URLClassLoader$1.run(URLClassLoader.java:361)
		at java.security.AccessController.doPrivileged(Native Method)
		at java.net.URLClassLoader.findClass(URLClassLoader.java:360)
		at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
		at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
		at java.lang.Class.forName0(Native Method)
		at java.lang.Class.forName(Class.java:340)
		at sun.rmi.server.Util.createStub(Util.java:292)
		... 19 more

The reason for this seems to be that
```java.rmi.server.UnicastRemoteObject.exportObject(Remote)``` uses
```sun.rmi.server.UnicastServerRef.UnicastServerRef(true)``` (see the
Java doc for the constructur for more details). I did some google-ing
and found this:

+ http://www.stratos.me/2008/05/stub-class-not-found/
	
+ http://stackoverflow.com/questions/10648026/why-the-class-cannot-be-seen-in-its-source-file-java
	
+ http://osdir.com/ml/java.sun.rmi/2006-10/msg00000.html

I still do not understand why there is a class
```com.sun.proxy.$Proxy0_Stub``` that is generated and loaded(?)  but
cannot be accessed afterwards. Anyway ...

### Controlling the client and server side sockets (Remote object/service)

Now let's run our ```Remote``` service on another host/IP. This time
the server-side has to tell the client how to connect through a ```java.rmi.server.RMIClientSocketFactory```:

	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-server2.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-server2 :ssf (new-ssf :addr "127.0.0.2") :csf (new-csf :addr "127.0.0.2"))' \
	  -e '@(promise)'

Run the client.

	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -e '(run-rmi-client)'

You'll get:

    [...]
	Caused by: java.rmi.UnmarshalException: error unmarshalling return; nested exception is:
			java.lang.ClassNotFoundException: user.proxy$java.lang.Object$Serializable$RMIClientSocketFactory$d954f6b8 \
			(no security manager: RMI class loader disabled)
			at sun.rmi.registry.RegistryImpl_Stub.lookup(Unknown Source)
    [...]

In this case our serialized ```proxy``` (instance) from ```new-csf```
made it to the ```rmi-client``` but its class ```user.proxy$java.lang.Object$Serializable$RMIClientSocketFactory$d954f6b8```
is not visible to the client. So the instance cannot be deserialized.

Clojure creates class-definitions **on-the-fly** for functions and
proxys (and other things) and hands those to the JVM. These classes
are not loaded from JARs/class-files (if you are not using AOT
compilation).

We can use the trick from above to make Clojure write those
class-definitions to the file-system from where the client can load
them - like so:

	example7$ mkdir -p ./class-cache/ \
      && rm -rf ./class-cache/* \
      && java -Dclojure.compile.path=./class-cache/ \
       -cp lib/clojure.jar clojure.main \
       -e '(.bindRoot Compiler/COMPILE_FILES true)' \
       -i clj/h42/rmi-server2.clj \
	   -i clj/h42/sf.clj \
	   -e '(run-rmi-server2 :ssf (new-ssf :addr "127.0.0.2") :csf (new-csf :addr "127.0.0.2"))' \
	   -e '@(promise)'

Run the client with ```./class-cache/``` on the classpath:

	example7$ java -cp ./class-cache/:lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -e '(run-rmi-client)'

This time you'll get:

	Exception in thread "main" java.lang.IllegalStateException: Attempting to call unbound fn: #'user/to-inet-addr
			at clojure.lang.Var$Unbound.throwArity(Var.java:43)
			at clojure.lang.AFn.invoke(AFn.java:39)
    [...]

The client did find/load the class but Clojures generated classes for
functions are just *references* to the corresponding functions
(i.e. the **```var``` which must be bound to the function**) --- in
this case ```#'user/to-inet-addr``` (which can be loaded from
```sf.clj```). And this ```var``` is not bound at the moment.

OK, let's fix that:

	example7$ java -cp ./class-cache/:lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-client)'

WTF?!?! Now we get (again):

    [...]
	Caused by: java.rmi.NoSuchObjectException: no such object in table
			at sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:276)
			at sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:253)
			at sun.rmi.server.UnicastRef.invoke(UnicastRef.java:162)
			at java.rmi.server.RemoteObjectInvocationHandler.invokeRemoteMethod(RemoteObjectInvocationHandler.java:194)
			at java.rmi.server.RemoteObjectInvocationHandler.invoke(RemoteObjectInvocationHandler.java:148)
			at com.sun.proxy.$Proxy0.voidMethod(Unknown Source)
    [...]

This again seems to be some kind of classloading issue. We fix this by
making the classes in ```./class-cache/``` available to the ```rmi-server```.

	example7$ mkdir -p ./class-cache/ \
      && rm -rf ./class-cache/* \
      && java -Dclojure.compile.path=./class-cache/ \
       -cp ./class-cache/:lib/clojure.jar clojure.main \
       -e '(.bindRoot Compiler/COMPILE_FILES true)' \
       -i clj/h42/rmi-server2.clj \
	   -i clj/h42/sf.clj \
	   -e '(run-rmi-server2 :ssf (new-ssf :addr "127.0.0.2") :csf (new-csf :addr "127.0.0.2"))' \
	   -e '@(promise)'

And once more:

	example7$ java -cp ./class-cache/:lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-client)'

Phew! Finally!

### Using the class-server for serving class-cache

The solution above works but the client and the server have to **share
a file-system** in order to access ```./class-cache/```. What if we
used the ```class-server``` to handle the access? Like this:

	example7$ java -cp ./class-cache/:lib/clojure.jar:bin/ clojure.main \
	  -i clj/h42/misc.clj \
	  -i clj/h42/class-server.clj \
	  -e '(run-class-server)'

Run the server:

	example7$ mkdir -p ./class-cache/ \
      && rm -rf ./class-cache/* \
      && java -Dclojure.compile.path=./class-cache/ \
       -cp lib/clojure.jar clojure.main \
       -e '(.bindRoot Compiler/COMPILE_FILES true)' \
       -i clj/h42/rmi-server2.clj \
	   -i clj/h42/sf.clj \
	   -e '(run-rmi-server2 :ssf (new-ssf :addr "127.0.0.2") :csf (new-csf :addr "127.0.0.2"))' \
	   -e '@(promise)'

And once more:

	example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-client)'

Oh no! Not again! We get:

    [...]
	Caused by: java.rmi.NoSuchObjectException: no such object in table
			at sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:276)
			at sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:253)
			at sun.rmi.server.UnicastRef.invoke(UnicastRef.java:162)
    [...]

For some weird reason this works when using ```run-rmi-server```
instead of ```run-rmi-server2```. I assume that it's a class-loading
issue again. While trying to find a **real** fix I discovered that
just adding

    (defmacro foo [])

at the top of ```rmi-server2.clj``` will make it work. I do not know
**why** this is so though. Must be some internal Clojure stuff.... OK,
we'll leave it and see how far we can get.

### Caching and serving dynamic class-definitions

Serving the generated classes via the ```class-server``` was ok for
checking if we could have the ```rmi-client``` *remote load* those
classes in principle.

Now we would like to *intercept* the generated classes within the
```rmi-server```, cache them somewhere and then serve them to remote
clients. This time the ```rmi-server``` and the ```class-server```
should be able to run with no file-system being shared.

First let's *grab* the generated classes. We do not want to use ```-e
'(.bindRoot Compiler/COMPILE_FILES true)'``` since we do not want
Clojure to write all class-definitions for all functions/proxys/etc to
the file-system. Instead we want to *selectivly* decide which
class-definitions to *grab*.

I did not find a way to tell Clojure to pass the generated byte-code
directly to a function (that I could supply). As a workaround the
macro ```class-def-for``` in ```class-def-for.clj``` first runs the
compile (of the body), reads the byte-code from the generated files
and finally deletes those files. The byte-code and the classname is
then passed to a function that the user of the macro supplies.

The byte-code may then be cached and could be delivered by a
```class-server```. For now we will run the ```class-server``` in the
```rmi-server```'s JVM. But we could extend the ```class-server``` so
that it can **receive** (and cache and deliver) byte-code remotely.

The ```class-def-for``` macro will first *compile* the body (by
calling the Clojure compiler explicitly) and then *return* the body
(to Clojures evaluation) so in the end the compiler will compile the
body **twice**.

I haven't found a way to do this with **just one** compile. And I do
not know what may result from this if the compile of the body had any
side-effects (other than writing the byte-code to the file-system).

So now when we run the ```rmi-server``` we have to wrap the code for
which we want to serve the class-definitions with ```class-def-for```
and hand-over the byte-code to the ```class-server```. We do this by
using ```caching-rec-fn``` which makes the class-definition avialable
to the ```class-server``` by calling ```put-cache``` (in ```class-server.clj```).

Our first try is this: (make sure that you have
```/tmp/class-cache/``` created --
i.e. ```${java.io.tmpdir}/class-cache/```)

	example7$ rm -rf /tmp/class-cache/* \
	   && java -cp lib/clojure.jar:./bin/ clojure.main \
       -i clj/h42/misc.clj \
       -i clj/h42/class-server.clj \
	   -e '(run-class-server)' \
	   -i clj/h42/class-def-for.clj \
       -i clj/h42/rmi-server2.clj \
       -e '(class-def-for caching-rec-fn (load-file "clj/h42/sf.clj"))' \
	   -e '(run-rmi-server2 :ssf (new-ssf :addr "127.0.0.2") :csf (new-csf :addr "127.0.0.2"))' \
	   -e '@(promise)'

Here we use ```(load-file "clj/h42/sf.clj")``` instead of ```-i
clj/h42/sf.clj``` to load the code. And the client:

    example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-client)'

You'll get a ```ClassNotFoundException```. If we put ```#_``` before
```(delete-tree! tmp-dir)``` in ```class-def-for.clj``` we should be
able to see which classes are generated. Repeating the run above
reveals that **no classes** are written to the file-system.

	example7$ find /tmp/class-cache/ 

What went wrong? 

We wrapped the code (body) that loads (and compiles) ```sf.clj``` with
a macro which **compiles** this body (at **macro-expansion time**
which is **compile-time**) in order to grab the classes. But the
compile of **this body** does not produce any classes! Its the
**evaluation of this body** which loads the code and finally leads to
class generation. So this is a case where we have to apply the *class
grabbing* at **evaluation-time** and not at **compile-time**.

Luckily we have the function ```class-def-for**``` in
```class-def-for.clj```for just this:

	example7$ rm -rf /tmp/class-cache/* \
	   && java -cp lib/clojure.jar:./bin/ clojure.main \
       -i clj/h42/misc.clj \
       -i clj/h42/class-server.clj \
	   -e '(run-class-server)' \
	   -i clj/h42/class-def-for.clj \
       -i clj/h42/rmi-server2.clj \
       -e '(class-def-for** caching-rec-fn #(load-file "clj/h42/sf.clj"))' \
	   -e '(run-rmi-server2 :ssf (new-ssf :addr "127.0.0.2") :csf (new-csf :addr "127.0.0.2"))' \
	   -e '@(promise)'

Now run the client:

    example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-client)'

OK, this works. And now we want to see the macro *in action*. For this
we use ```sfx.clj```. This file has the same content as ```sf.clj```
with the code being wrapped with ```class-def-for```.

	example7$ rm -rf /tmp/class-cache/* \
	   && java -cp lib/clojure.jar:./bin/ clojure.main \
       -i clj/h42/misc.clj \
       -i clj/h42/class-server.clj \
	   -e '(run-class-server)' \
	   -i clj/h42/class-def-for.clj \
       -i clj/h42/rmi-server2.clj \
       -i clj/h42/sfx.clj \
	   -e '(run-rmi-server2 :ssf (new-ssf :addr "127.0.0.2") :csf (new-csf :addr "127.0.0.2"))' \
	   -e '@(promise)'

Then run the client:

    example7$ java -cp lib/clojure.jar clojure.main \
	  -i clj/h42/rmi-client.clj \
	  -i clj/h42/sf.clj \
	  -e '(run-rmi-client)'

Again you'll get a ```ClassNotFoundException```. When you run this
with the *disabled deletion* (see above) you should be able to see
that ```find /tmp/class-cache/``` does indeed show some generated
classes. But there seem to be classes which are missing. The cause
seems to be the *double compilation* mentioned above: the second
compile generates those classes for which instances are bound to the
RMI runtime and thus those classes are not *grabed* and cannot be seen
by the ```class-server``` and the ```rmi-client```.

I haven't found a fix for this. So currently only the function
```class-def-for**``` will work but not the macro ```class-def-for```.

**TODO: link to stackoverflow "serializing clojure functions"**

## Using the Java inter-op

The ```run-rmi-server``` from above uses reflection
(```Class/forName``` and
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
**compile** time (i.e. macro expansion when the ```proxy``` macro is
expanded!) before the function is executed (at **eval** time when the
```(.setContextClassLoader (Thread/currentThread) cl)``` is executed).

But there is a simple trick: **set the classloader before the form is
compiled**. You could do this by first running ```set-ctccl.clj```
(you may remove the call to ```.setContextClassLoader``` from
```run-rmi-server2``` for this run if you like):

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

**[TODO: explain how to use bind-loader-root.clj]**



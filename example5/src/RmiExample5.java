import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiExample5 {

	private final static int RMI_DEFFAULT_PORT = 1099;

	private static class Client {
		public static void main(String[] args) throws Exception {

			int rmiRegistryPort = RMI_DEFFAULT_PORT;
			System.out.println("Connecting to RMI registry on port "
					+ rmiRegistryPort);
			Registry registry = LocateRegistry.getRegistry(rmiRegistryPort);

			String codeBase = "http://127.0.0.1:8080/class-server/";
			ClassLoader cl = new URLClassLoader(new URL[] { new URL(codeBase) });
			Thread.currentThread().setContextClassLoader(cl);

			String id = args.length == 0 ? "RmiExample6.MyService" : args[0];
			System.out.println("Looking up RMI stub '" + id + "'");
			Remote stub = registry.lookup(id);

			System.out.println("Received stub " + stub);
			
			Method voidMethod = stub.getClass().getMethod("voidMethod");
			System.out.println("Calling " + voidMethod + " on the stub.");
			voidMethod.invoke(stub);

			System.out.println("Done.");
		}
	}
}

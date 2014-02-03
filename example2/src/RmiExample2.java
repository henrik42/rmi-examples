import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RmiExample2 {

	private final static int RMI_DEFFAULT_PORT = 1099;

	interface MyService extends Remote {

		String RMI_ID = MyService.class.getCanonicalName();
		int PORT = 0;

		void voidMethod() throws RemoteException;
	}

	public static class MyServiceRmiImpl implements MyService {

		@Override
		public void voidMethod() {
			System.out.println("executing voidMethod");
		}

	}

	private static class Server {
		public static void main(String[] args) throws Exception {

			MyService myService = new MyServiceRmiImpl();

			System.out.println("exporting " + myService + " on port "
					+ MyService.PORT);
			MyService myServiceStub = (MyService) UnicastRemoteObject
					.exportObject(myService, MyService.PORT);

			boolean connectToRegistry = args.length > 0
					&& "connect".equals(args[0]);

			final Registry registry;
			if (connectToRegistry) {
				System.out.println("Connecting to RMI registry on port "
						+ RMI_DEFFAULT_PORT);
				registry = LocateRegistry.getRegistry(RMI_DEFFAULT_PORT);
			} else {
				System.out.println("Starting RMI registry on port "
						+ RMI_DEFFAULT_PORT);
				registry = LocateRegistry.createRegistry(RMI_DEFFAULT_PORT);
			}

			System.out.println("Binding " + myServiceStub + " with id "
					+ MyService.RMI_ID + " to RMI registry");
			registry.rebind(MyService.RMI_ID, myServiceStub);

			System.out.println("Waiting for incoming calls...");
		}
	}

	private static class Client {
		public static void main(String[] args) throws Exception {

			int rmiRegistryPort = RMI_DEFFAULT_PORT;
			System.out.println("Connecting to RMI registry on port "
					+ rmiRegistryPort);
			Registry registry = LocateRegistry.getRegistry(rmiRegistryPort);

			System.out.println("Looking up service of type "
					+ MyService.class.getSimpleName() + " with id "
					+ MyService.RMI_ID);
			MyService myService = (MyService) registry.lookup(MyService.RMI_ID);

			System.out.println("Calling voidMethod() on " + myService);
			myService.voidMethod();

			System.out.println("Done.");
		}
	}
}

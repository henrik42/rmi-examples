import java.io.IOException;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.UnicastRemoteObject;

/**
 * NOTE: this won't work. The code is for debugging and playing around.
 */
public class RmiExample4 {

	private final static int RMI_DEFFAULT_PORT = 1099;

	interface MyService extends Remote {

		String RMI_ID = MyService.class.getCanonicalName();
		int PORT = 6666;

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

			System.out.println(myServiceStub + " is waiting incoming calls...");
		}
	}

	private static class Client {
		public static void main(String[] args) throws Exception {

			RMIClientSocketFactory csf = new RMIClientSocketFactory() {

				@Override
				public Socket createSocket(String host, int port)
						throws IOException {
					return new Socket("127.0.0.1", MyService.PORT);
				}

				@Override
				public boolean equals(Object obj) {

					return obj != null && this.getClass() == obj.getClass();
				}

				@Override
				public int hashCode() {
					return 1;
				}
			};

			MyService myService = (MyService) UnicastRemoteObject.exportObject(
					new MyServiceRmiImpl(), 0, csf, null);

			System.out.println("Calling voidMethod() on " + myService);
			myService.voidMethod();

			System.out.println("Done.");
		}
	}
}

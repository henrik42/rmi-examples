import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;

public class RmiExample1 {

	interface MyService extends Remote {

		String ID = MyService.class.getCanonicalName() + ".ser";
		int PORT = 6666;

		void voidMethod() throws RemoteException;
	}

	public static class MyServiceRmiImpl implements MyService {

		@Override
		public void voidMethod() {
			System.out.println(this + " executing voidMethod");
		}

	}

	public static class Server {
		public static void main(String[] args) throws Exception {

			MyService myService = new MyServiceRmiImpl();

			System.out.println("exporting " + myService + " on port "
					+ MyService.PORT);
			MyService myServiceStub = (MyService) UnicastRemoteObject
					.exportObject(myService, MyService.PORT);

			System.out.println("Exporting serialized service stub "
					+ myServiceStub + " to file " + MyService.ID);
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(MyService.ID));
			oos.writeObject(myServiceStub);
			oos.close();

			System.out.println("Waiting for incomming calls...");

		}
	}

	private static class Client {
		public static void main(String[] args) throws Exception {

			System.out.println("Reading service stub for "
					+ MyService.class.getName() + " from file " + MyService.ID);
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					MyService.ID));
			MyService myService = (MyService) ois.readObject();
			ois.close();

			System.out.println("Calling voidMethod() on " + myService);
			myService.voidMethod();

			System.out.println("Done.");
		}
	}
}

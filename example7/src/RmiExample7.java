import java.rmi.Remote;
import java.rmi.RemoteException;

public class RmiExample7 {

	public interface MyService extends Remote {

		void voidMethod() throws RemoteException;
	}

}

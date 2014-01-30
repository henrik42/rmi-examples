package foo;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface MyService extends Remote {

	void voidMethod() throws RemoteException;
}

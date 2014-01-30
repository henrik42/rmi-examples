import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiRegistry {

	private final static int RMI_DEFFAULT_PORT = 1099;

	public static void main(String[] args) throws Exception {

		System.out
				.println("Starting RMI registry on port " + RMI_DEFFAULT_PORT);
		Registry registry = LocateRegistry.createRegistry(RMI_DEFFAULT_PORT);
		System.out.println("Registry " + registry + " ready.");

		Thread.sleep(999999);

	}
}

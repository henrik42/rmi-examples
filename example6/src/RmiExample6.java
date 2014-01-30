import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class RmiExample6 {

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

	public static class ClassServer {

		private static class ClassAccessHandler implements HttpHandler {

			final Pattern m_pattern;
			final ClassLoader classLoader = Thread.currentThread()
					.getContextClassLoader();

			ClassAccessHandler(String pPattern) {
				m_pattern = Pattern.compile(pPattern);
			}

			private void consumeRequestData(HttpExchange pExchange)
					throws IOException {

				InputStream is = pExchange.getRequestBody();
				byte[] buffer = new byte[8192];
				while (is.read(buffer) != -1)
					;
			}

			private byte[] loadInputStream(InputStream pIs) throws IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[8192];
				for (int i; (i = pIs.read(buffer)) != -1; baos.write(buffer, 0,
						i))
					;
				return baos.toByteArray();
			}

			@Override
			public void handle(HttpExchange exchange) throws IOException {

				URI requestUri = exchange.getRequestURI();
				System.out.println("Received request " + requestUri);

				// see HttpExchange java doc
				consumeRequestData(exchange);

				Matcher matcher = m_pattern.matcher(requestUri.toString());
				if (!matcher.matches())
					throw new RuntimeException("Request " + requestUri
							+ " does not match " + m_pattern);

				String classNameArg = matcher.group(1);
				URL[] hits = Collections.list(
						classLoader.getResources(classNameArg)).toArray(
						new URL[0]);

				if (hits.length == 0) {
					System.out.println("Found no class defs for "
							+ classNameArg);
					exchange.close();
					return;
				}

				System.out.println("Found class defs for " + classNameArg
						+ " at " + Arrays.toString(hits));

				final InputStream classDataInputStream = hits[0].openStream();
				byte[] classData = loadInputStream(classDataInputStream);
				classDataInputStream.close();

				System.out.println("Returning " + classData.length
						+ " bytes as class defintion.");
				exchange.sendResponseHeaders(200, classData.length);

				InputStream is = new ByteArrayInputStream(classData);
				try {
					OutputStream os = exchange.getResponseBody();
					byte[] buffer = new byte[8192];
					for (int i; (i = is.read(buffer)) != -1; os.write(buffer,
							0, i))
						;
				} finally {
					exchange.close();
				}
			}
		}

		public static void main(String[] args) throws Exception {

			InetSocketAddress isa = new InetSocketAddress("127.0.0.1", 8080);
			System.out.println("Starting HTTP class server on " + isa);

			HttpServer server = HttpServer.create(isa, 0);

			server.createContext("/class-server/", new ClassAccessHandler(
					"/class-server/(.*)"));

			server.start();

			System.out
					.println("Class server is waiting for incoming calls ...");
		}
	}

	private static class Server {
		public static void main(String[] args) throws Exception {

			ClassServer.main(new String[0]);

			MyService myService = new MyServiceRmiImpl();

			System.out.println("exporting " + myService + " on port "
					+ MyService.PORT);
			MyService myServiceStub = (MyService) UnicastRemoteObject
					.exportObject(myService, MyService.PORT);

			boolean connectToRegistry = args.length > 0
					&& "connect".equals(args[0]);

			Registry registry;
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
}

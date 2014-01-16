import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatServer {
	// The socket people will connect to.
	private ServerSocket connection;
	// Map of name to IP address.
	private HashMap<String, InetAddress> mapOfConnections;
	// List of name in the chat room.
	private List<String> names;
	
	public ChatServer() {
		makeServerSocket();
		mapOfConnections = new HashMap<>();
		names = new ArrayList<>();
	}
	
	/**
	 * Start listening for connections and create
	 * two threads for each connection. One to update
	 * the available connections, and one to give the user
	 * the IP addresses as per request.
	 */
	public void listen() {
		while (true) {
			try {
			 Socket incoming = connection.accept();
			 // Register the user.
			 Thread t = new Thread(new RegisterThread(incoming));
			 t.start();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void makeServerSocket() {
		try {
			connection = new ServerSocket(8888);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Registers the user and 
	 * creates two threads to talk with him.
	 * @author Raffi
	 *
	 */
	private class RegisterThread implements Runnable {
		// The socket of our client.
		private Socket incoming;
		
		public RegisterThread(Socket s) {
			incoming = s;
		}
		
		/**
		 * Get the name of the user and create two new threads.
		 * One to update the uses with other users he can connect to,
		 * and another to send him IP addresses of the users
		 * he wishes to connect to.
		 */
		@Override
		public void run() {
			try {
				ObjectOutputStream out = new ObjectOutputStream(incoming.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(incoming.getInputStream());
				String name = (String) in.readObject();
				// Check if the username has been taken.
				while (names.contains(name)) {
					// notify user of an error
					out.writeObject(null);
					// Get new username.
					name = (String) in.readObject();
				}
				// add name to the list.
				names.add(name);
				// Put the IP info in the map.
				mapOfConnections.put(name, incoming.getInetAddress());
				Thread t = new Thread(new TalkingThread(name, out, in));
				t.start();
				Thread t2 = new Thread(new UpdateThread(out));
				t2.start();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	/**
	 * Constantly checks for updates for the user.
	 * @author Raffi
	 *
	 */
	private class UpdateThread implements Runnable {
		// Stream to output to the user.
		ObjectOutputStream out;
		// Number we will use to check if 
		// the client is full updated already.
		int registered;
		public UpdateThread(ObjectOutputStream out) {
			this.out = out;
			registered = 1;
		}
		
		/**
		 * Check if the user needs to be updated.
		 */
		@Override
		public void run() {
			try (ObjectOutputStream out = this.out) {
				while (true) {
					if (names.size() != registered) {
						// Update the amount registered.
						registered = names.size();
						// Give the client an updates list.
						out.writeObject(names.toArray());
					}
					try {
						// Do this again every second.
						Thread.sleep(1000);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				}
			}
		}
	
	/**
	 * Registers the user with the server,
	 * and waits for the user to request to
	 * register with someone.
	 * @author Raffi
	 *
	 */
	private class TalkingThread implements Runnable {
		private String username;
		private ObjectOutputStream out;
		private ObjectInputStream in;
		
		
		public TalkingThread(String username, ObjectOutputStream out, ObjectInputStream in) {
			this.username = username;
			this.out = out;
			this.in = in;
		}
		
		/**
		 * Wait for reader input, and send the client
		 * the requested IP addresses.
		 */
		@Override
		public void run() {
			try (ObjectOutputStream out = this.out) {			
				while (true) {
					Object[] list = (Object[]) in.readObject();
					InetAddress [] addresses = new InetAddress[list.length];
					for (int i = 0; i < addresses.length; i++ ) {
						addresses[i] = mapOfConnections.get(list[i]); 
					}
					out.writeObject(addresses);
				}  
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				mapOfConnections.remove(username);
				names.remove(username);	
			}
		}	
	}
	public static void main(String[] args) {
		ChatServer c = new ChatServer();
		c.listen();
	}
}

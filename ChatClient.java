import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ChatClient extends JFrame {
	private static final long serialVersionUID = -8811629567434435454L;
	private static final int DEFAULT_WIDTH = 400;
	private static final int DEFAULT_HEIGHT = 450;
	
	// Map that contains list of connections and their
	// associated output stream.
	Map<String, ObjectOutputStream> connectedMap;
	// Socket to connect to server.
	Socket client;
	// Outputstream to get ip addresses
	ObjectOutputStream outStream;
	// input stream to retrieve ip addresses
	ObjectInputStream inStream;
	// JLists to hold available and connected users.
	JList<String> availableConnectionsJList;
	JList<String> connectedJList;
	// Models for available and connected users.
	DefaultListModel<String> connectedlListModel;
	DefaultListModel<String> availableListModel;
	// JButton to connects to users.
	JButton connectButton;
	// Delete connection button.
	JButton disconnectButton;
	// Button to send messages.
	JButton sendButton;
	// JTextarea for the chat.
	JTextArea chatSpace;
	// The JTextarea to enter a message to send.
	JTextArea composeMessageSpace;
	// The user name.
	String username;
	// Panels to hold the components.
	JPanel composeMessageSpacePanel;
	JPanel availableConnectionsPanel;
	JPanel connectedPanel;
	JPanel allConnnectionsPanel;
	
	public ChatClient() {
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		username = JOptionPane.showInputDialog(null,"Please enter your username");
	    availableListModel = new DefaultListModel<>();
		availableConnectionsJList = new JList<>(availableListModel);
		availableConnectionsJList.setLayoutOrientation(JList.VERTICAL);
		availableConnectionsJList.setFixedCellWidth(100);
		
		connectedlListModel = new DefaultListModel<>();
		connectedJList = new JList<>(connectedlListModel);
		connectedJList.setLayoutOrientation(JList.VERTICAL);
		connectedJList.setFixedCellWidth(100);
		
		
		JScrollPane availableScroll = new JScrollPane(availableConnectionsJList);
		JScrollPane connectedScroll = new JScrollPane(connectedJList);
		
		connectButton = new JButton("Connect");
		connectButton.addActionListener(new IPActionListener());
		
		disconnectButton = new JButton("Disconnect");
		disconnectButton.addActionListener(new DisconnectAction());
		
		availableConnectionsPanel = new JPanel(new BorderLayout());
		availableConnectionsPanel.add(availableScroll, BorderLayout.CENTER);
		availableConnectionsPanel.add(connectButton, BorderLayout.SOUTH);
		
		connectedPanel = new JPanel(new BorderLayout());
		connectedPanel.add(connectedScroll, BorderLayout.CENTER);
		connectedPanel.add(disconnectButton, BorderLayout.SOUTH);
		
		allConnnectionsPanel = new JPanel(new BorderLayout());
		allConnnectionsPanel.add(availableConnectionsPanel, BorderLayout.CENTER);
		allConnnectionsPanel.add(connectedPanel, BorderLayout.SOUTH);
		
		chatSpace = new JTextArea();
		chatSpace.setLineWrap(true);
		chatSpace.setEditable(false);
		JScrollPane chatScroll = new JScrollPane(chatSpace);
		
		composeMessageSpace = new JTextArea(5,20);
		composeMessageSpace.setLineWrap(true);
		JScrollPane enterScroll = new JScrollPane(composeMessageSpace);
		
		sendButton = new JButton("Send");
		sendButton.addActionListener(new SendAction());
		
		composeMessageSpacePanel = new JPanel();
		composeMessageSpacePanel.add(enterScroll);
		composeMessageSpacePanel.add(sendButton);
		
		add(allConnnectionsPanel, BorderLayout.EAST);
		add(chatScroll, BorderLayout.CENTER);
		add(composeMessageSpacePanel, BorderLayout.SOUTH);
		
		// Use a synchronized map for the connected users.
		connectedMap = Collections.synchronizedMap(new HashMap<String, ObjectOutputStream>());
		
		makeConnection();
	}
	
	/**
	 * Connect to the server and make this client into a server
	 * to connects to other users.
	 */
	public void makeConnection() {
		try {
			client = new Socket("Raffi-HP", 8888);
			inStream = new ObjectInputStream(client.getInputStream());
			outStream = new ObjectOutputStream(client.getOutputStream());
			// Send username to the server.
			outStream.writeObject(username);
			// Now make the client a server to connect to other users.
			Thread t = new Thread(new ServerThread());
			t.start(); 
			// Get the server input, IP addresses and updated available connections.
			Thread t2 = new Thread(new ServerInput());
			t2.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get information sent by the server. This
	 * includes IP addresses and available users,
	 */
	public void getServerInput() {
		try {
			
			Object o = inStream.readObject();
			// Check if the server sent us a duplicate
			// user name notification.
			if (o == null) {
				newUserName();
				outStream.writeObject(username);
			}
			else {
				Object [] info = (Object[]) o;
				// Check if list of available users was sent.
				if (info[0] instanceof String) {
					// Update JList with new information.
					updateAvailableConnections(info);
				}
				else {
					// We got the IP addresses of users to connect to.
					// Connect to them.
					makeChat(info);
				}
			}
		}
		catch (Exception e) {
				e.printStackTrace();
				
		}
	}
	
	/**
	 * Prompt user for a new username.
	 */
	private void newUserName() {
		username = JOptionPane.showInputDialog(null,"Please enter a different username.\n" +
				                                    "The requested username has already been taken"); 
	}

	/**
	 * Make a chat with the users we requested
	 * to connect to.
	 * @param addresses IP addresses of the user.
	 */
	private void makeChat(Object[] addresses) {
		for (Object o : addresses) {
			InetAddress address = (InetAddress) o;
			// Check if the server sent us a good IP.
			if (o != null) {
				try {
					// Connect to the requested user by connected
					// to their machine.
					Socket s = new Socket(address,9999);
					Thread t = new Thread(new ChatThread(s));
					t.start();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Update the available connections we can 
	 * connect to.
	 * @param names The names of available users.
	 */
	private void updateAvailableConnections(Object[] names) {
		// Clear the JList.
		availableListModel.clear();
		// Create a list out of the array of names.
		List<Object> availableConnections = new ArrayList<Object>(Arrays.asList(names)); 
		// remove from available connections the users we are already conneceted to.
		availableConnections.removeAll(connectedMap.keySet());
		for (Object name : availableConnections) {
			// If its not this user.
			if (!username.equals(name)) {
				availableListModel.addElement((String) name);
			}
		}
	}
	
	/**
	 * Runnable class that makes the client
	 * into a server waiting for connections.
	 * @author Raffi
	 *
	 */
	private class ServerThread implements Runnable {
		// Server socket for people to connect to.
		ServerSocket server;
		public ServerThread() {
			makeServerSocket();
		}
		public void makeServerSocket() {
			try {
				server = new ServerSocket(9999);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * When a user is connected create a 
		 * chat thread with the user.
		 */
		@Override
		public void run() {
			while (true) {
				try {
					Socket s = server.accept();
					// Create the connection with the user that connected.
					Thread t = new Thread(new ChatThread(s));
					t.start();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Runnable class that creates a chat between 
	 * two users.
	 * @author Raffi
	 *
	 */
	private class ChatThread implements Runnable {
		// The socket to use to communicate.
		private Socket s;
		// Name of the user we are connected to.
		private String nameOfConnection;
		
		public ChatThread(Socket s) {
			this.s = s;
		}
		
		/**
		 * Creates connection with the other user.
		 */
		@Override
		public void run() {
			try (ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
				// Send your username to othe user.
				out.writeObject(username);
				// Get connection's username
				nameOfConnection = (String) in.readObject();
				// Add connection to the map
				connectedMap.put(nameOfConnection, out);
				// Add him to the connected JList.
				connectedlListModel.addElement(nameOfConnection);
				// Remove connection from the available connections JList.
				availableListModel.removeElement(nameOfConnection);
				// Send an initial connected message to the connection.
				out.writeObject("Connected to " + username + '\n');
				// Keep on checking if more messgaes have been sent.
				while (true) {
					String message = (String) in.readObject();
					// Add the message to the chat space.
					chatSpace.append(message);
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				// When the connection is lost and we were connected.
				if (nameOfConnection != null) {
					// Remove connection from connected JList.
					connectedlListModel.removeElement(nameOfConnection);
					// Remove him from our map of connections.
					connectedMap.remove(nameOfConnection);
					// Put him back into available users.
					availableListModel.addElement(nameOfConnection);
					// Note in the chat area that he has been disconnected.
					chatSpace.append("Disconnected from " + nameOfConnection + '\n'); 
				}
			}
		}
	}
	
	/**
	 * Runnable to constantly check information sent by the server.
	 * @author Raffi
	 *
	 */
	private class ServerInput implements Runnable {

		@Override
		public void run() {
			while (true) {
				getServerInput();
			}	
		}
	}
	
	/**
	 * ActionListener to respond to a connectButton click.
	 * Gets IP addresses from the server.
	 * @author Raffi
	 *
	 */
	private class IPActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			List<String> list = availableConnectionsJList.getSelectedValuesList();
			try {
				if (list.size() > 0) {
				// Write out the names we want to connect to.
				outStream.writeObject(list.toArray());
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ActionListener to send a message to available users.
	 * @author Raffi
	 *
	 */
	private class SendAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String message = username + ": " + composeMessageSpace.getText().trim() + '\n';
			// Get all of our connections' output streams.
			Collection<ObjectOutputStream> receivers = connectedMap.values();
			synchronized (connectedMap) {
				Iterator<ObjectOutputStream> it = receivers.iterator();
				while (it.hasNext()) {
					ObjectOutputStream out = it.next();
					try {
						out.writeObject(message);
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			// Send it to yourself if you have connections.
			if (connectedMap.size() > 0) {
				chatSpace.append(message);
			}
			composeMessageSpace.setText("");
		}
	}
	
	
	/**
	 * ActionListener to disconnect from connected users.
	 * @author Raffi
	 *
	 */
	private class DisconnectAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			// For every user we wish to disconnect from
			for (String name : connectedJList.getSelectedValuesList()) {
				// close the connection between us
				ObjectOutputStream out = connectedMap.get(name);
				try {
					out.close();
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				ChatClient c = new ChatClient();
				c.setTitle("Raffi's Chatroom");
				c.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				c.setVisible(true);
			}
		});	
	}
}

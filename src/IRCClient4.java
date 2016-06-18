import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
/*-----------------------------------*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
/*-----------------------------------*/
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.InputMismatchException;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
/*-----------------------------------*/
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/*
 * for irc reference: https://tools.ietf.org/html/rfc2812
 */
public class IRCClient4 {
	static String serverName = "4chan";
	static PrintWriter out;
	static Socket serverEnd;
	static BufferedReader in;
	static TreeMap<String, JTextPane> tabs = new TreeMap<String, JTextPane>();
	static JTabbedPane tabPanel;
	static JFrame consoleFrame;
	static String nick;
	// for testing purposes
	static boolean trigger = false;
	static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public static void main(String[] args) throws IOException {
		consoleFrame = makeConsole();
		while (!trigger) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		doStuff();
	}

	public static void doStuff() throws IOException {
		// instantiates stuff to store statements
		String fromServer = "";
		while (serverEnd.isConnected()) {
			// reads input from server if server is ready
			if (in.ready()) {
				fromServer = in.readLine();
				if (serverName.equals("4chan"))
					serverName = fromServer.split(" ")[0].substring(fromServer.split(" ")[0].indexOf('.') + 1,
							fromServer.split(" ")[0].lastIndexOf('.'));
				try {
					// TODO that thing V
					loadToPanel(serverName, parseServerStuff(fromServer) + "\n");
				} catch (Exception e) {
					loadToPanel(serverName, "Oops something broke");
					throw new IOException();
				}
			}
		}
		serverEnd.close();
		in.close();
		out.close();
		System.exit(0);
	}

	public static JMenuBar makeMenu() {
		ActionListener q = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				switch (arg0.getActionCommand()) {
				case "connect":
					try {
						connect();
						trigger = true;
					} catch (InputMismatchException | IOException e) {
						e.printStackTrace();
					}
					break;
				case "quit":
					System.exit(0);
					break;
				default:
					JOptionPane.showMessageDialog(null, "Unknown error: Nonexistant button pressed");
				}
			}
		};
		JMenuBar bars = new JMenuBar();
		JMenu start = new JMenu("start");
		JMenuItem connect = new JMenuItem("connect");
		JMenuItem quit = new JMenuItem("quit");
		connect.addActionListener(q);
		quit.addActionListener(q);
		start.add(connect);
		start.add(quit);
		bars.add(start);
		return bars;
	}

	// does stuff with server input
	public static String parseServerStuff(String fromServer) throws Exception {
		String[] cmdz = fromServer.split(" ");
		// replies to pings
		Calendar cal = Calendar.getInstance();
		String time = dateFormat.format(cal.getTime());
		if (cmdz[0].equals("PING")) {
			// TODO actually implement pong
			loadToPanel(serverName, "PONG :" + fromServer.split(":")[1] + "\n");
			out.println("PONG :" + fromServer.split(":")[1] + "\n");
			return "*pinges client*";
		} else if (cmdz[1].equals("PRIVMSG")) {
			System.out.println("fromServer: " + fromServer);
			String text = fromServer.substring(fromServer.indexOf(':'));
			if (cmdz[2].charAt(0) == '#') {
				loadToPanel(cmdz[2], time + " | " + cmdz[0].substring(1, cmdz[0].indexOf('!')) + " : " + text);
			} else {
				loadToPanel(cmdz[0].substring(1, cmdz[0].indexOf('!')), time + " | " + text);
				return "Message: " + text.substring(1) + " from " + cmdz[0];
			}
			System.out.println("?");
			return "Message: " + text.substring(1) + " from " + cmdz[0] + " on the channel " + cmdz[2];
		} else if (cmdz[1].equals("JOIN")) {}
		return fromServer;
	}

	public static void parseClientStuff(String fromClient) throws Exception {
		String[] cmdz = fromClient.split(" ");
		String currPanel = tabPanel.getTitleAt(tabPanel.getSelectedIndex());
		if (cmdz[0].charAt(0) == '/') {
			out.println(fromClient.substring(1) + "\n");
			loadToPanel(serverName, fromClient.substring(currPanel.equals(serverName) ? 0 : 1) + "\n");
			if (cmdz[0].substring(1).toLowerCase().equals("join")) {
				loadToPanel(cmdz[1].toLowerCase(), "");
			}
		} else {
			loadToPanel(nick + " | " + tabPanel.getTitleAt(tabPanel.getSelectedIndex()), fromClient);
			out.println("PRIVMSG " + tabPanel.getTitleAt(tabPanel.getSelectedIndex()) + " :" + fromClient);
		}
	}

	// basic handshake with server
	private static void register() {
		nick = "";
		String realName = "";
		while (realName.equals("") || nick.equals("")) {
			nick = JOptionPane.showInputDialog("Enter your username");
			realName = JOptionPane.showInputDialog("Enter your real name");
		}
		out.println("NICK " + nick);
		loadToPanel(serverName, "NICK " + nick);
		out.println("USER " + nick + " somewhere " + "overtherainbow :" + realName);
		loadToPanel(serverName, "USER " + nick + " somewhere " + "overtherainbow :" + realName);
	}

	// sets up iostreams
	private static void connect() throws UnknownHostException, IOException {
		// sets up the client socket
		String hostName = JOptionPane.showInputDialog("Enter host url");
		int portNumber = Integer.parseInt(JOptionPane.showInputDialog("Enter host port"));
		serverEnd = new Socket(hostName, portNumber);
		// sets up the irc account
		try {
			out = new PrintWriter(serverEnd.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(serverEnd.getInputStream()));
			register();
			loadToPanel(serverName, "Connected to :" + hostName + " on port " + portNumber + "\n");
		} catch (UnknownHostException e) {
			System.err.println("Host not found :" + hostName);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + hostName);
			System.exit(1);
		}
	}

	private static JFrame makeConsole() throws IOException {
		// Creates Console
		Dimension temp = new Dimension((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2,
				(int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2);
		consoleFrame = new JFrame("Console");
		consoleFrame.setLocation((int) temp.getWidth() / 2, (int) temp.getHeight() / 2);
		consoleFrame.getContentPane().setLayout(null);
		consoleFrame.setDefaultCloseOperation(3);
		consoleFrame.setPreferredSize(temp);
		consoleFrame.setResizable(false);
		consoleFrame.setSize(temp);
		System.out.println(consoleFrame.getSize());
		//
		makeTabPane();
		// creates input box
		JTextField input = new JTextField();
		input.setLocation((int) (temp.getWidth() / 2 - 175), (int) (temp.getHeight() - (temp.getHeight() / 8)) - 30);
		input.setPreferredSize(new Dimension(350, 30));
		input.setSize(new Dimension(350, 30));
		input.setEditable(true);
		input.setText("");
		input.getInputMap(JTextField.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Entered");
		input.getActionMap().put("Entered", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					parseClientStuff(input.getText().replaceAll("<b>", "\u0003").replace("</.+>", "\u0000"));
					// loadToPanel(serverName, nick + ": " + input.getText() +
					// "\n");
				} catch (Exception e1) {
					e1.printStackTrace();
				} finally {
					input.setText("");
				}
			}
		});
		// adds previously made stuff
		consoleFrame.setJMenuBar(makeMenu());
		consoleFrame.add(input);
		consoleFrame.add(tabPanel);
		consoleFrame.pack();
		consoleFrame.setVisible(true);
		return consoleFrame;
	}

	public static void loadToPanel(String panelName, String loadText) {
		panelName = panelName.toLowerCase();
		if (tabs.get(panelName) == null) {
			tabPanel.add(panelName, new JScrollPane(makeNewTab(panelName)));
		}
		try {
			HTMLEditorKit kit = (HTMLEditorKit) tabs.get(panelName).getEditorKit();
			HTMLDocument doc = (HTMLDocument) tabs.get(panelName).getDocument();
			kit.insertHTML(doc, doc.getLength(), loadText, 0, 0, null);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
	}

	public static JScrollPane makeNewTab(String name) {
		if (tabs.get(name) != null) {
			return null;
		}
		// Initialized the writing pane
		JTextPane output = new JTextPane();
		output.setEditorKit(new HTMLEditorKit());
		output.setDocument(new HTMLDocument());
		output.setEditable(false);
		((DefaultCaret) output.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		output.setContentType("text/html");
		// Allows html links to be registered as links
		output.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		output.setVisible(true);
		// output.setText("There should be something here");
		// Dumps this into the tabs set
		tabs.put(name, output);
		// Wraps writing pane into a scroll pane
		Dimension temp = new Dimension((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2,
				(int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2);
		JScrollPane outputScroll = new JScrollPane(output);
		outputScroll.setSize(
				new Dimension((int) temp.getWidth() - 15, (int) (temp.getHeight() - (temp.getHeight() / 8)) - 30));
		outputScroll.setAlignmentX(0);
		outputScroll.setVisible(true);
		return outputScroll;
	}

	public static void makeTabPane() {
		tabPanel = new JTabbedPane();
		tabPanel.setSize(new Dimension((int) consoleFrame.getWidth() - 30,
				(int) (consoleFrame.getHeight() - (consoleFrame.getHeight() / 8)) - 30));
		tabPanel.addTab(serverName, makeNewTab(serverName));
	}
}
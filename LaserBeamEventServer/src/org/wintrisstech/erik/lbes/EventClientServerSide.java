package org.wintrisstech.erik.lbes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EventClientServerSide implements Runnable {

	private final EventServer server;
	// private final BufferedReader in;
	private final PrintWriter out;
	private final Socket socket;
	private boolean listening = true;

	public EventClientServerSide(EventServer server, Socket socket, PrintWriter out) {
		this.server = server;
		this.out = out;
		this.socket = socket;
		new Thread(this).start();
	}

	@Override
	public void run() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Listening for input from " + socket.getRemoteSocketAddress());
			String line = null;
			while (listening && (line = in.readLine()) != null) {
				System.out.println(line);
				if (line.toLowerCase().equals("bye")) {
					listening = false;
				}
			}
			server.sayGoodBye(this);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		}
	}

	public PrintWriter getOut() {
		return out;
	}

	public Socket getSocket() {
		return socket;
	}

	public void shutdown() {
		try {
			if (socket != null) {
				socket.close();
			}
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}

}

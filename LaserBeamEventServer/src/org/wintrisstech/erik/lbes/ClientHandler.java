package org.wintrisstech.erik.lbes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

	private final EventServer server;
	// private final BufferedReader in;
	private final PrintWriter out;
	private final Socket socket;
	private boolean listening = true;

	public ClientHandler(EventServer server, Socket socket) throws IOException {
		this.server = server;
		this.out = new PrintWriter(socket.getOutputStream(), true);
		this.socket = socket;
		new Thread(this).start();
	}

	@Override
	public void run() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = null;
			while (listening && (line = in.readLine()) != null) {
				listening = !line.toLowerCase().equals("bye");
			}
			server.removeHandler(this);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public PrintWriter getOut() {
		return out;
	}

	public Socket getSocket() {
		return socket;
	}

}

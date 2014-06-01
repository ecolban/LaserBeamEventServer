package org.wintrisstech.erik.lbes;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class EventServer {

	public static final int PORT = 47047;
	private boolean listening = true;

	private List<EventClientServerSide> clients = new ArrayList<EventClientServerSide>();
	private GpioController gpio;

	public static void main(String[] args) {
		EventServer server = new EventServer();
		server.initialize();
		server.listenForClients();
	}

	private void initialize() {

		// create gpio controller
		gpio = GpioFactory.getInstance();

		// provision gpio pin #02 as an input pin with its internal pull down
		// resistor enabled
		final GpioPinDigitalInput laserBeam = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02,
				PinPullResistance.PULL_DOWN);

		// create and register gpio pin listener
		laserBeam.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				boolean newState = event.getState() == PinState.HIGH;
				for (EventClientServerSide cl : clients) {
					cl.getOut().println(newState);
				}
			}
		});

		System.out.println("Initialization complete");
	}

	private void listenForClients() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
			System.out.println("Listening for clients on " + serverSocket.getLocalPort());
			while (listening) {
				final Socket socket = serverSocket.accept();
				EventClientServerSide client = new EventClientServerSide(
						this, socket, new PrintWriter(socket.getOutputStream(), true));
				clients.add(client);
				System.out.println("Connected to " + socket.getRemoteSocketAddress());
			}
		} catch (IOException e) {
			System.out.println("Exception caught while listening on port " + PORT);
			System.out.println(e.getMessage());
		} finally {
			for (EventClientServerSide client : clients) {
				client.shutdown();
			}
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
			gpio.shutdown();
		}
	}

	public void sayGoodBye(EventClientServerSide client) throws IOException {
		String clientAddress = client.getSocket().getRemoteSocketAddress().toString();
		client.getOut().println("bye");
		clients.remove(client);
		client.shutdown();
		System.out.println("Disconnected from " + clientAddress);
	}
}

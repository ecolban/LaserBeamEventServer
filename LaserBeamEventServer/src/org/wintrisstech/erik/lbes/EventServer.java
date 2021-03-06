package org.wintrisstech.erik.lbes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class EventServer {

	public static final int PORT = 47047;
	private boolean listening = true;

	private boolean localHigh = false;
	private boolean remoteHigh = false;

	private List<ClientHandler> clientHandlers = new ArrayList<ClientHandler>();
	private GpioController gpio;

	public static void main(String[] args) {
		EventServer server = new EventServer();
		server.initialize();
		server.listenForClients();
	}

	private void initialize() {

		// create gpio controller
		gpio = GpioFactory.getInstance();

		// Set up a LED indicator
		// Provision gpio pin #10 as an output pin
		final GpioPinDigitalOutput watchdog = gpio.provisionDigitalOutputPin(
				RaspiPin.GPIO_10, PinState.LOW);
		watchdog.blink(getBlinkDelay(), PinState.HIGH);
		// provision gpio pin #02 as a local input pin with its internal pull
		// down resistor enabled
		final GpioPinDigitalInput laserBeamSenseLocal = gpio.provisionDigitalInputPin(
				RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);

		// provision gpio pin #11 as a remote input pin with its internal pull
		// down resistor enabled
		final GpioPinDigitalInput laserBeamSenseRemote = gpio.provisionDigitalInputPin(
				RaspiPin.GPIO_11, PinPullResistance.PULL_DOWN);

		// create and register local GPIO pin listener
		laserBeamSenseLocal.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				localHigh = event.getState() == PinState.HIGH;
				for (ClientHandler cl : clientHandlers) {
					cl.getOut().println("local:" + (localHigh ? "high" : "low"));
				}
				watchdog.blink(getBlinkDelay(), PinState.HIGH);
			}

		});

		// create and register remote GPIO pin listener
		laserBeamSenseRemote.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				remoteHigh = event.getState() == PinState.HIGH;
				for (ClientHandler cl : clientHandlers) {
					cl.getOut().println("remote:" + (remoteHigh ? "high" : "low"));
				}
				watchdog.blink(getBlinkDelay(), PinState.HIGH);
			}
		});
		System.out.println("Initialization complete");
	}

	/**
	 * Sets the led to blink at a rate that depends on the state of the two
	 * laser beams.
	 * 
	 * @param led
	 *            the pin of the LED that is set to blink
	 */
	private long getBlinkDelay() {
		return localHigh
				? (remoteHigh ? 1000L : 500L)
				: (remoteHigh ? 500L : 250L);
	}

	private void listenForClients() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
			System.out.println("Listening on " + serverSocket.getLocalPort());
			while (listening) {
				addHandler(serverSocket.accept());
			}
		} catch (IOException e) {
			System.out.println("Exception caught while listening on port " + PORT);
			System.out.println(e.getMessage());
		} finally {
			for (ClientHandler handler : clientHandlers) {
				handler.getOut().println("bye");
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

	private void addHandler(final Socket socket) throws IOException {
		ClientHandler clientHandler = new ClientHandler(this, socket);
		clientHandlers.add(clientHandler);
		System.out.println("Connected to " + socket.getRemoteSocketAddress());
	}

	public void removeHandler(ClientHandler handler) throws IOException {
		SocketAddress clientAddress = handler.getSocket().getRemoteSocketAddress();
		clientHandlers.remove(handler);
		System.out.println("Disconnected from " + clientAddress);
	}
}

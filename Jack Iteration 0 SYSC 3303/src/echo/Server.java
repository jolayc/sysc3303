package echo;

/**
 * EchoServer
 * Server side of a simple echo server.
 * The server receives from a client a request packet sent from the intermediate host
 * It then sends a reply packet back to the intermediate host.
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */

import java.net.*;

public class Server implements Runnable{
	
	private int port;
	
	private boolean running = true;
	
	private DatagramSocket receiveSocket;
	
	protected Thread listener;
	
	public Server(){
		this.port = 69;
		try {
			//Constructs a socket to receive packets bounded to port 69
			receiveSocket = new DatagramSocket(port);
		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void run() {
		
			synchronized(this){
				this.listener = Thread.currentThread();
			}
			while(this.running()){
				new Thread(new ServerThread(receiveSocket)).start();
			}
			shutdown();
		}
	
	public synchronized void stop(){
		this.running = true;
		receiveSocket.close();
	}
	
	public synchronized boolean running(){
		return this.running;
	}
	
	private synchronized void shutdown() {
		receiveSocket.close();
		System.out.println("Server has stopped taking requests.");
		while (true) {} // allows for transfers in progress (before shutdown) to complete
	}
	
	public static void main( String args[] ){
		Server s = new Server();
		s.run();
	}

	
}

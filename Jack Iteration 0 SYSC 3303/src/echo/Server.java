package echo;

import java.net.*;
import java.util.Scanner;

/**
 * Server
 * Server side of a simple echo server.
 * The server receives from a client a request packet sent from the intermediate host
 * It then sends a reply packet back to the intermediate host.
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */

public class Server implements Runnable{
	
	private int port;
	private boolean running;	
	private DatagramSocket receiveSocket;
	protected Thread listener;
	
	public Server(){
		this.port = 69;
		running = true;
		try {
			//Constructs a socket to receive packets bounded to port 69
			receiveSocket = new DatagramSocket(port);
		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Runs the server
	 */
	public synchronized void run() {
		
			System.out.println("Server: print exit to exit");
			
			synchronized(this){
				this.listener = Thread.currentThread();
			}
			
			while(isRunning()){
				new Thread(new ServerThread(receiveSocket)).start();
				
			}
			shutdown();
	}
	
	/**
	 * Stops the running loop
	 */
	public synchronized void stop(){
		running = false;
	}
	
	public synchronized boolean isRunning(){
		return this.running;
	}
	
	/**
	 * Shuts down the server
	 */
	private synchronized void shutdown() {
		receiveSocket.close();
		System.out.println("Server has stopped taking requests.");
	}
	
	public static void main( String args[] ){
		Server server = new Server();
		new Thread(server).start();
		System.out.println("Hee");
		Scanner scan = new Scanner(System.in);
		if(scan.hasNextLine()){
			String message = scan.nextLine();
			if(message.equals("exit")){
				scan.close();
				server.stop();
			}
		}
	}
}

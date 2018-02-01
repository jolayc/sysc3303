package iteration1;

import java.util.Scanner;

/**
 * A class (thread) responsibly for gracefully exiting
 * the Server
 */
public class ServerExit extends Thread {
	private server s;
	
	public ServerExit(String name, server s) {
		super(name);
		this.s = s;
	}
	
	/**
	 * Waits for user to enter 'exit' and then closes the 
	 * Server socket, does not accept any more requests
	 * and lets any transfers in progress to finish
	 */
	public void run() {
		Scanner sc = new Scanner(System.in);
		String in;
		while (true) {
			System.out.println("Server: To exit, enter 'exit'");
			in = sc.nextLine().toLowerCase();
			if(in.equals("exit")) break;
		}
		sc.close();
		s.stop();
	}
}

/*
 * A sample client that uses DatagramService
 */

package applications;

import java.io.IOException;
import java.io.PrintWriter;

import datatypes.Datagram;
import services.DatagramService;

public class client {

	private static DatagramService ds;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		if(args.length != 3) {
			printUsage();
		}

		System.out.println("Starting client ...");

		int port = Integer.parseInt(args[0]);
		ds = new DatagramService(port, 10);
		String fileName = args[2];

		Datagram datagram = new Datagram();
		datagram.setData(args[2]);
		datagram.setSrcaddr("127.0.0.1");
		datagram.setDstaddr("127.0.0.1");
		datagram.setDstport((short)Integer.parseInt(args[1]));
		datagram.setSrcport((short)port);

		ds.sendDatagram(datagram);
		System.out.println("Sent Request for File");

		datagram = ds.receiveDatagram();
		if (datagram.getData() != null) {
			System.out.println("Received File");

			//Store the data received in the localDisk
			PrintWriter out = new PrintWriter("clientFiles/" + fileName);
			out.print(datagram.getData());
			out.close();
			System.out.println("Done saving the file");
		} else {
			System.out.println("No such file found on the server");
		}
	}

	private static void printUsage() {
		System.out.println("Need to pass 3 Command Line arguments: <localport> <serverport> <filename>\n");
		System.exit(-1);
	}
}

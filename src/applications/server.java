/*
 * A sample server that uses DatagramService
 */
package applications;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import datatypes.Datagram;
import services.DatagramService;

public class server {

	private static DatagramService ds;

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		if(args.length != 1) {
			printUsage();
		}

		System.out.println("Starting Server ...");
		int port = Integer.parseInt(args[0]);
		ds = new DatagramService(port, 10);
		run();
	}

	private static void run() throws IOException, ClassNotFoundException {

		Datagram datagram;

		while(true) {
			datagram = ds.receiveDatagram();
			System.out.println("Received datagram from " + datagram.getSrcaddr() + ":" + datagram.getSrcport() + " Data: " + datagram.getData());
			Datagram fileData = new Datagram();
			fileData.setSrcaddr(datagram.getDstaddr());
			fileData.setSrcport(datagram.getDstport());
			fileData.setDstaddr(datagram.getSrcaddr());
			fileData.setDstport(datagram.getSrcport());
			fileData.setData(getFileContents(datagram.getData().toString()));
			ds.sendDatagram(fileData);
		}
	}

	private static Object getFileContents(String fileName) {
		try {
			BufferedReader br = new BufferedReader(new FileReader("serverFiles/" +  fileName));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			br.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void printUsage() {
		System.out.println("Usage: server <port>");
		System.exit(-1);
	}
}

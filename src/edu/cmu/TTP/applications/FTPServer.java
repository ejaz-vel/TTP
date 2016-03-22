/*
 * A sample server that uses DatagramService
 */
package edu.cmu.TTP.applications;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.services.TTPService;

public class FTPServer {

	private static TTPService ttp;

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

		if(args.length != 1) {
			printUsage();
		}

		System.out.println("Starting Server ...");
		int port = Integer.parseInt(args[0]);
		ttp = new TTPService(port);
		run();
	}

	private static void run() throws IOException, ClassNotFoundException, InterruptedException {
		Datagram datagram;
		while(true) {
			
			// Wait for a Syn.
			System.out.println("Waiting for a SYN packet");
			datagram = ttp.receiveDatagram();
			if(((TTPSegment)datagram.getData()).getType().equals(PacketType.SYN)) {
				System.out.println("Received SYN datagram from " + datagram);
				ttp.sendAck(datagram, null);
				
				datagram = ttp.receiveDatagram();
				System.out.println("Received Data datagram from " + datagram);
				
				Datagram fileData = new Datagram();
				fileData.setSrcaddr(datagram.getDstaddr());
				fileData.setSrcport(datagram.getDstport());
				fileData.setDstaddr(datagram.getSrcaddr());
				fileData.setDstport(datagram.getSrcport());
				fileData.setData(getFileContents(datagram.getData().toString()));
				ttp.sendData(fileData);
			}
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

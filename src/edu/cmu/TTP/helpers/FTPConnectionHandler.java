package edu.cmu.TTP.helpers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import edu.cmu.TTP.models.ClientPacketID;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.services.TTPService;

public class FTPConnectionHandler implements Runnable {

	private ConcurrentMap<ClientPacketID, Datagram> map;
	private Datagram synDatagram;
	private TTPService ttp;

	public FTPConnectionHandler(ConcurrentMap<ClientPacketID, Datagram> map, Datagram datagram, TTPService ttp) {
		this.map = map;
		this.synDatagram = datagram;
		this.ttp = ttp;
	}

	@Override
	public void run() {
		try {
			ClientPacketID clientData = new ClientPacketID();
			clientData.setIPAddress(synDatagram.getSrcaddr());
			clientData.setPort(synDatagram.getSrcport());
			clientData.setPacketType(PacketType.DATA_REQ_SYN);
			
			while (!map.containsKey(clientData));
			
			System.out.println("Received Filename in thread");
			Datagram datagram = map.get(clientData);
			map.remove(clientData);
			
			Datagram fileData = new Datagram();
			fileData.setSrcaddr(datagram.getDstaddr());
			fileData.setSrcport(datagram.getDstport());
			fileData.setDstaddr(datagram.getSrcaddr());
			fileData.setDstport(datagram.getSrcport());
			fileData.setData(getFileContents(new String(((TTPSegment)datagram.getData()).getData())));
			ttp.sendData(fileData, map);
		} catch(IOException | ClassNotFoundException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private Object getFileContents(String fileName) {
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
}

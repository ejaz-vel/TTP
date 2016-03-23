package edu.cmu.TTP.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentMap;

import edu.cmu.TTP.models.ClientPacketID;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.services.TTPService;

/**
 * @author ejaz
 * 
 * 
 *
 */
public class FTPConnectionHandler implements Runnable {
	private ConcurrentMap<ClientPacketID, Datagram> map;
	private Datagram synDatagram;
	private TTPService ttp;

	public FTPConnectionHandler(ConcurrentMap<ClientPacketID, Datagram> map,
			Datagram datagram, TTPService ttp) {
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
			while (!map.containsKey(clientData))
				;
			System.out.println("Received Filename in thread");
			Datagram datagram = map.get(clientData);
			map.remove(clientData);
			Datagram fileData = new Datagram();
			fileData.setSrcaddr(datagram.getDstaddr());
			fileData.setSrcport(datagram.getDstport());
			fileData.setDstaddr(datagram.getSrcaddr());
			fileData.setDstport(datagram.getSrcport());
			fileData.setData(getFileContents(
					new String(((TTPSegment) datagram.getData()).getData())));
			ttp.sendData(fileData, map);
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private byte[] getFileContents(String fileName) {
		try {
			return Files.readAllBytes(Paths.get("serverFiles/" + fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

/**
 * 
 */
package edu.cmu.TTP.models;

/**
 * @author apurv
 *
 */
public class ConnectionEssentials {

	private String clientAddress;
	private String serverAddress;
	private short clientPort;
	private short serverPort;
	
	public ConnectionEssentials(String clientAddress, String serverAddress, int clientPort, int serverPort) {
		super();
		this.clientAddress = clientAddress;
		this.serverAddress = serverAddress;
		this.clientPort = (short)clientPort;
		this.serverPort = (short)serverPort;
	}

	public String getClientAddress() {
		return clientAddress;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public short getClientPort() {
		return clientPort;
	}

	public short getServerPort() {
		return serverPort;
	}

	@Override
	public String toString() {
		return "ConnectionEssentials [clientAddress=" + clientAddress + ", serverAddress=" + serverAddress
				+ ", clientPort=" + clientPort + ", serverPort=" + serverPort + "]";
	}
}

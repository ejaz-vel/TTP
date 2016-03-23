/**
 * 
 */
package edu.cmu.TTP.models;

/**
 * @author apurv Types of packets that we are handling. Note - Packet types are
 *         defined by the flags that are set. These are not standard packet
 *         types.
 */
public enum PacketType {
	SYN, ACK, DATA_REQ_SYN, DATA_REQ_ACK, DATA, DATA_ACK, FIN, FIN_ACK
}

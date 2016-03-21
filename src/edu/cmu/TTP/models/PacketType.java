/**
 * 
 */
package edu.cmu.TTP.models;

/**
 * @author apurv
 * Types of packets that we are handling. 
 * Note - Packet types are defined by the flags that are set. DATA is not a standard TCP packet type.
 */
public enum PacketType {
	SYN,SYN_ACK,ACK,DATA_REQ_SYN,DATA_REQ_ACK,DATA,DATA_ACK
}
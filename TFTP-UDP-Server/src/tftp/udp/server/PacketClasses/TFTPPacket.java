package tftp.udp.server.PacketClasses;

import java.nio.ByteBuffer;
import tftp.udp.server.TFTPException;

/**
 * @author 184514
 * @date 03/05/2020
 */
public abstract class TFTPPacket {

    /**
     * Constructor creates a TFTPPacket from the Raw Byte Data.
     * 
     * @param buffer - Byte array of data to be stored in packet
     * @param len - Length of the data 
     * @return TFTPPacket Storing data from the byte array
     * @throws TFTPException Thrown when packet type not recognised
     */
    public static TFTPPacket fromByteArray(byte[] buffer, int len) throws TFTPException {
        /*Opcode used in PacketType class to match the opcode presented with the
        packetType declared in the enumeration. */
        short opcode = ByteBuffer.wrap(buffer).getShort();
        PacketType packetType = PacketType.fromOpcode(opcode);
        /*Depending on the packetType, the correct packet should be created. i.e. 
        Constrcutor for that packetType should be called, and supplied with correct args. */
        switch (packetType) {
            //If packetType returned from fromOpcode() is READ, create a RRQ Packet etc.
            case READ:
                return new RRQPacket(buffer, len);
            case WRITE:
                return new WRQPacket(buffer, len);

            case DATA:
                return new DataPacket(buffer, len);
            case ACK:
                return new AckPacket(buffer, len);
            case ERROR:
                return new ErrorPacket(buffer, len);
            //Invalid packetTypes lead to an exception thrown. Also thrown in packetType enum.
            default:
                throw new TFTPException("Unknown packet type: " + packetType);
        }
    }

    /**
     * An abstract method which will be implemented by all the child classes 
     */
    public abstract byte[] getPacketBytes();
    public abstract PacketType getPacketType();
}

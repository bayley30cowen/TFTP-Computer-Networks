package tftp.udp.client.PacketClasses;

import java.nio.ByteBuffer;
/**
 *
 * @author 184514
 */
public class AckPacket extends TFTPPacket {

    private final int blockNo, PACKET_LEN = 4;
    /*Two bytes for the opcode, other two bytes for block number*/
    private final byte[] packetBytes;

    /**
     * Creates a new AckPacket (Acknowledgement Packet) from block number.
     * @param blockNo block number of packet
     */
    public AckPacket(int blockNo) {
        this.blockNo = blockNo;
        this.packetBytes = new byte[PACKET_LEN];
        ByteBuffer buffer = ByteBuffer.wrap(packetBytes);
        buffer.putShort((short) getPacketType().getOpcode());
        buffer.putShort((short) blockNo);
    }
    
    /**
     * Constructor creates a new AckPacket from the byte data.
     * @param dataPacket byte array of data to be stored in packet
     * @param len length of byte data
     */
    public AckPacket(byte[] dataPacket, int len){
        ByteBuffer buffer = ByteBuffer.wrap(dataPacket);
        buffer.position(2);
        this.blockNo = buffer.getInt();
        this.packetBytes = new byte[len];
        System.arraycopy(dataPacket, 0, packetBytes, 0, len);
    }

    /**
     * Returns the packet data as byte array.
     * @return byteArray of packet data
     */
    @Override
    public byte[] getPacketBytes() {
        return packetBytes;
    }
    
    /**
     * Returns the type of packet.  (ACK)
     * @return packetType 
     */
    @Override
    public PacketType getPacketType() {
        return PacketType.ACK;
    }
    
    /**
     * Returns the block number of ACKPacket.
     * @return value of blockNo
     */
    public int getBlockNo() {
        return blockNo;
    }
    
    
    
    
    
}

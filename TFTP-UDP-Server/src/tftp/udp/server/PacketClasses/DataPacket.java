package tftp.udp.server.PacketClasses;

import java.nio.ByteBuffer;

/**
 *
 * @author 184514
 */
public class DataPacket extends TFTPPacket {
    
    /*Two bytes for the opcode, other two bytes for block number*/
    private final short blockNo;
    private final byte[] packetBuffer;
    //Constant
    public static final int DATA_OFFSET = 4;
    private final int dataLen;

    /**
     * Creates a DataPacket using the block number, the Byte data and 
     * Length of the Byte data.
     * 
     * @param blockNo block number of packet
     * @param dataBuffer data to be stored in packet
     * @param dataLen length of data to be stored
     */

    public DataPacket(short blockNo, byte[] dataBuffer, int dataLen) {
        this.blockNo = blockNo;
        this.dataLen = dataLen;
        this.packetBuffer = new byte[dataLen + DATA_OFFSET];
        ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(blockNo);
        buffer.put(dataBuffer, 0, dataLen);
        
}
    
    /**
     * Creates a Data Packet from the Byte Data.
     * 
     * @param dataInPacket data to be stored in packet
     * @param len length of data to be stored in packet
     */
    public DataPacket(byte[] dataInPacket, int len){
        ByteBuffer buf = ByteBuffer.wrap(dataInPacket);
        buf.position(2);
        this.blockNo = buf.getShort();
        this.dataLen = len - DATA_OFFSET;
        this.packetBuffer = new byte[len];
        System.arraycopy(dataInPacket, 0, this.packetBuffer, 0, len);
    }

    /**
     * Returns Packet data as raw bytes.
     * @return byte array of the data held in packet
     */
    @Override
    public byte[] getPacketBytes() {
        return packetBuffer;
    }
    
    /**
     * Returns packet type (DATA).
     * @return returns packet type
     */
    @Override
    public final PacketType getPacketType() {
        return PacketType.DATA;
    }

    /**
     * Returns Block Number of the data packet.
     * @return value of blockNum variable
     */
    public short getBlockNumber() {
        return blockNo;
    }
    
    /**
     * This method checks the length of  the data. Used to identify if the packet 
     * is final packet.
     *
     * @return True if and only if, the data is of less than 512 bytes.
     */
    public boolean isFinalPacket(){
        return dataLen < 512;
    }
    
    /**
     * Returns the length of data in the packet.
     *
     * @return Length of data in packet
     */
    public int getPacketLength() {
        return dataLen;
    }
    
    
}

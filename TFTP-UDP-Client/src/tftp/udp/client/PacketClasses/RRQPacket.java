package tftp.udp.client.PacketClasses;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author 184514
 * @date 03/05/2020
 */
public class RRQPacket extends TFTPPacket {

    private final String fname;
    private final String mode = "octet";
    private final byte[] bytes;

    /**
     * Creates a Read Request (RRQ) Packet using the filename.
     *
     * @param filename - Name of the file to be written to.
     */
    public RRQPacket(String filename) {
        this.fname = filename;
        byte[] fnameBytes = getBytes(filename);
        byte[] modBytes = getBytes(mode);
        this.bytes = new byte[fnameBytes.length + modBytes.length + 2];
        ByteBuffer buffer = ByteBuffer.wrap(this.bytes);
        buffer.putShort((short) getPacketType().getOpcode());
        buffer.put(fnameBytes);
        buffer.put(modBytes);
    }

    /**
     * Creates a Read Request (RRQ) Packet using the raw byte data.
     *
     * @param dataInPacket  - Array (Byte) of the data to be stored in packet
     * @param len len of the packet
     */
    public RRQPacket(byte[] dataInPacket, int len) {
        this.fname = getString(dataInPacket, 2);
        int offset = 2;
        while (dataInPacket[offset] != 0 && offset < len) {
            offset++;
        }
        offset++;
        this.bytes = new byte[len];
        System.arraycopy(dataInPacket, 0, this.bytes, 0, len);

    }

    /**
     * Using the Filename retrieves the Byte Array.
     *
     * @param fname - Filename to convert to byte data
     * @return byte array of data from the filename (fname)
     */
    private byte[] getBytes(String fname) {
        //US_ASCII used instead of NET_ASCII as JAVA does not support natively!
        //US_ASCII however is a subset of NET_ASCII
        byte[] bytesArray = fname.getBytes(StandardCharsets.US_ASCII);
        byte[] bytesData = new byte[bytesArray.length + 1];
        System.arraycopy(bytesArray, 0, bytesData, 0, bytesArray.length);
        bytesData[bytesData.length - 1] = 0;
        return bytesData;
    }

    /**
     * Returns Byte data from the Packet.
     *
     * @return byte array of data from packet
     */
    @Override
    public byte[] getPacketBytes() {
        return bytes;
    }

    /**
     * Returns Packet Type.
     *
     * @return Packet type (READ)
     */
    @Override
    public final PacketType getPacketType() {
        return PacketType.READ;
    }
    
    /**
     * Returns the string using the Raw data within the packet.
     *
     * @param dataInPacket - Byte data to be converted (into string)
     * @param offset - Position of data within packet to be converted
     * @return String
     */
    private String getString(byte[] dataInPacket, int offset) {
        //offset used to store start position, nullPos used to store end position.
        int nullPos = offset;
        while (nullPos < dataInPacket.length && dataInPacket[nullPos] != 0) {
            nullPos++;
        }
        //Length of String equals end position - start position 
        int len = nullPos - offset;
        return new String(dataInPacket, offset, len, StandardCharsets.US_ASCII);
    }
    
    /**
     * Returns mode of the packet.
     * 
     * @return Value mode of the packet. For this case, always octet
     */
    public String getMode() {
        return mode;
    }
    
    /**
     * Returns Filename of the file to read.
     * 
     * @return value of filename variable
     */
    public String getFilename() {
        return fname;
    }

}

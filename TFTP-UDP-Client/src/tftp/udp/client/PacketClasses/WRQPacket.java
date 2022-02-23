package tftp.udp.client.PacketClasses;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author 184514
 * @date 03/05/2020
 */
public class WRQPacket extends TFTPPacket {

    private final String fname; //filename
    private final String mode = "octet";
    private final byte[] bytes;

    /**
     * Constructor creates a Write Request (WRQ) Packet using the name of the file.
     *
     * @param fname - The name of the file to be written to.
     */
    public WRQPacket(String fname) {
        this.fname = fname;
        byte[] fnBytes = getBytes(fname);
        byte[] modeBytes = getBytes(mode);
        //Size of bytes array needs to be larger than (len of fname + len of mode)
        this.bytes = new byte[fnBytes.length + modeBytes.length + 2];
        ByteBuffer buffer = ByteBuffer.wrap(this.bytes);
        buffer.putShort((short) getPacketType().getOpcode());
        //Adds to buffer bytes of mode and fname
        buffer.put(fnBytes);
        buffer.put(modeBytes);
    }

    /**
     * Alternate Constructor creates a Write Request (WRQ) Packet from the Raw Byte Data.
     *
     * @param dataInPacket - Data to be stored in the packet
     * @param len - Length of the packet
     */
    public WRQPacket(byte[] dataInPacket, int len) {
        /*Filename is the set to a string value of dataInPacket. 
        Done by converting scalar value  to a string.*/
        this.fname = getString(dataInPacket, 2);
        //Start Position of Mode.
        int modeOffset = 2;
        while (dataInPacket[modeOffset] != 0 && modeOffset < len) {
            modeOffset++;
        }
        modeOffset++;
        //Bytes stores the len of packet.
        this.bytes = new byte[len];
        /*Copies dataInPacket array from start pos 0 to bytes array start pos 0. 
        Len number of elements copied.*/
        System.arraycopy(dataInPacket, 0, this.bytes, 0, len);

    }

    /**
     * Returns Packet Type. (WRITE)
     *
     * @return Packet type
     */
    public PacketType getPacketType() {
        return PacketType.WRITE;
    } 
    
    /**
     * Returns the Byte array of data using the filename.
     *
     * @param filename - File to convert to into byte data
     * @return Byte array of data
     */
    private byte[] getBytes(String filename) {
        /*US_ASCII used instead of NET_ASCII as JAVA does not support natively!
        US_ASCII however is a subset of NET_ASCII*/
        byte[] bytesArray = filename.getBytes(StandardCharsets.US_ASCII);
        byte[] byteData = new byte[bytesArray.length + 1];
        //Copies bytesArray into byteData
        System.arraycopy(bytesArray, 0, byteData, 0, bytesArray.length);
        //Sets last element in array to 0.
        byteData[byteData.length - 1] = 0;
        return byteData;
    }

    /**
     * Returns Request packet mode. (Always Octet)
     * 
     * @return Value of mode of packet.
     */
    public String getMode() {
        return mode;
    }    
    
    /**
     * Returns the Filename.
     * 
     * @return value stored in filename (fname) variable
     */
    public String getFilename() {
        return fname;
    }

    /**
     * Gets the byte data using the packet.
     *
     * @return A byte array of data
     */
    @Override
    public byte[] getPacketBytes() {
        return bytes;
    }

    /**
     * Returns the String using the Packet Data.
     *
     * @param dataInPacket raw byte data to convert to string
     * @param offset position of data in packet to be converted to string
     * @return String of data from packet
     */
    private String getString(byte[] dataInPacket, int offset) {
        //Offset is the start pos. nullPos will be used to find and store end pos i.e. the null position.
        int nullPos = offset;
        /*While the nullPos is less than the len of data within packet &
        doesnt point to a location equallling 0*/
        while (nullPos < dataInPacket.length && dataInPacket[nullPos] != 0) {
            nullPos++;
        }
        //Length of the string is the end position - start position.
        int len = nullPos - offset;
        return new String(dataInPacket, offset, len, StandardCharsets.US_ASCII);
    }
}

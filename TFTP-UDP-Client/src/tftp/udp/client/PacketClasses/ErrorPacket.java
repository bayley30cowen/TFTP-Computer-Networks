package tftp.udp.client.PacketClasses;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author 184514
 * @date 02/05/2020
 */
public class ErrorPacket extends TFTPPacket {

    private final ErrorCodes errorCode;
    private final String errorMessage;
    private final byte[] bytes;

    //Enumeration of the different error codes
    public enum ErrorCodes {

        UNDEFINED(0, "Undefined error, check error message!"),
        FILE_NOT_FOUND(1, "File not found!"),
        ACCESS_VIOLATION(2, "Access violation!"),
        DISK_FULL(3, " Disk full / allocation exceeded!"),
        ILLEGAL_TFTP_OPERATION(4, "Illegal TFTP operation!"),
        UNKNOWN_TID(5, "Unknown transfer ID!"),
        FILE_EXISTS(6, "File exists already!"),
        NO_USER(7, "No such user!");
        private int code;
        private String message;

        ErrorCodes(int value, String message) {
            this.code = value;
            this.message = message;
        }

        /**
         * Returns the error code.
         *
         * @return value stored in the variable code 
         */
        public int getErrorCode() {
            return code;
        }

        /**
         * Returns the type of error using the error code given.
         *
         * @param code - error code
         * @return ErrorCode given by code
         */
        public static ErrorCodes fromErrorCode(int code) {
            //If the error code given is between 1 and 7, returns error type.
            for (ErrorCodes error : values()) {
                if (error.code == code) {
                    return error;
                }
            }
            //Else returns type undefined.
            return UNDEFINED;
        }

    }

    /**
     * Constructor that creates an Error packet from the Error code and Error message.
     *
     * @param eCode  - code of error to be stored in packet
     * @param eMessage - message to be stored in error packet
     */
    public ErrorPacket(ErrorCodes eCode, String eMessage) {
        this.errorCode = eCode;
        this.errorMessage = eMessage;
        byte[] msgBytes = getBytes(eMessage);
        this.bytes = new byte[msgBytes.length + 4];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putShort((short) getPacketType().getOpcode());
        buffer.putShort((short) eCode.getErrorCode());
        buffer.put(msgBytes);
    }

    /**
     * Constructor that creates an Error packet from the raw Byte data.
     *
     * @param bytes  - Byte array of data to be stored in packet
     * @param len - Length of data to be stored in packet. Used as size of byte array.
     */
    public ErrorPacket(byte[] bytes, int len) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(2);
        this.errorCode = ErrorCodes.fromErrorCode(buffer.getShort());
        this.errorMessage = getString(bytes, 4);
        this.bytes = new byte[len];
        System.arraycopy(bytes, 0, this.bytes, 0, len);
    }

    /**
     * Returns the Error message of error packet.
     *
     * @return String value of error message declared in the enum.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the type of packet. (ERROR)
     *
     * @return packetType
     */
    public PacketType getPacketType() {
        return PacketType.ERROR;
    }
    
    /**
     * Returns the packet data as raw bytes.
     *
     * @return byte array of packet data
     */
    @Override
    public byte[] getPacketBytes() {
        return bytes;
    }

    /**
     * Converts the data in the packets from bytes into type String.
     *
     * @param dataInPacket - Data (bytes) to be converted to string
     * @param offset - start position of byte data to be converted
     * @return String
     */
    private String getString(byte[] dataInPacket, int offset) {
        int nullPos = offset;
        while (nullPos < dataInPacket.length && dataInPacket[nullPos] != 0) {
            nullPos++;
        }
        int length = nullPos - offset;
        return new String(dataInPacket, offset, length, StandardCharsets.US_ASCII);
    }

    /**
     * Converts message from type String to Byte data.
     *
     * @param message - String to be converted to byte array
     * @return byte array of message
     */
    private byte[] getBytes(String message) {
        byte[] bytesArray = message.getBytes(StandardCharsets.US_ASCII);
        byte[] byteData = new byte[bytesArray.length + 1];
        System.arraycopy(bytesArray, 0, byteData, 0, bytesArray.length);
        byteData[byteData.length - 1] = 0;
        return byteData;
    }

}

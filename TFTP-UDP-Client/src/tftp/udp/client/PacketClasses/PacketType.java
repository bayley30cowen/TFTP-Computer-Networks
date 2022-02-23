package tftp.udp.client.PacketClasses;

import tftp.udp.client.TFTPException;

/**
 *
 * @author 184514
 */
public enum PacketType {
    /* Enumeration (Enum) is used (over a variable) as the packets must be one of the 
    following types Therefore Opcode of 4 is a packet of type ACK.*/
    READ("RRQ", 1),
    WRITE("WRQ", 2),
    DATA("DATA", 3),
    ACK("ACK", 4),
    ERROR("ERROR", 5);

    /**
     * Returns the type of packet from the given Opcode.
     *
     * @param opcode opcode of packet type to be returned
     * @return packet type with the given opcode
     * @throws TFTPException thrown when no packet type has the given opcode
     */
    static PacketType fromOpcode(short opcode) throws TFTPException {
        for (PacketType type : values()) {
            if (type.opcode == opcode) {
                return type;
            }
        }
        throw new TFTPException("Opcode not found: " + opcode + " Should be between (1 & 5)");
    }
    private String packetType;
    private short opcode;

    /**
     * Constructor initialises a new PacketType object.
     *
     * @param packetType type of packet to be created
     * @param opcode opcode of packet to be created
     */
    PacketType(String packetType, int opcode) {
        this.packetType = packetType;
        this.opcode = (short) opcode;
    }

    /**
     * Returns the opcode of packet.
     *
     * @return opcode value (between 1 - 5)
     */
    public short getOpcode() {
        return opcode;
    }

}

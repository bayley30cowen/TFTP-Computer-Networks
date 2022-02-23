package tftp.udp.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import tftp.udp.server.PacketClasses.*;

/**
 * @author 184514
 * @date 04/05/2020
 */
public class RRQHandler implements Runnable {

    private final int portOfClient;
    private final InetAddress addressOfClient;
    RRQPacket rrqPKT;
    //Constants, Timeout value random number in ms.
    private final int PCKT_LEN = 516, DATA_LEN = 512, MAX_AMOUNT_TIMEOUTS = 15, TIMEOUT = 7000;

    /**
     * Constructor creates a new Read Request (RRQ) Handler.
     *
     * @param portClient - Port to be used to connect to the client.
     * @param addressClient - Client's address.
     * @param rrqpckt - Packet to be sent from the client
     */
    public RRQHandler(int portClient, InetAddress addressClient, RRQPacket rrqpckt) {
        this.portOfClient = portClient;
        this.addressOfClient = addressClient;
        this.rrqPKT = rrqpckt;
    }

    /**
     * This method to be ran when a thread is started. Will continue to takes commands until exit is called.
     */
    @Override
    public void run() {
        System.out.println("A Read Request (RRQ) has been received: " + rrqPKT + " from " + addressOfClient + " : " + portOfClient);
        try {
            //Creates a new Datagram socket and sets the Timeout to 7000ms
            DatagramSocket sckt = new DatagramSocket();
            sckt.setSoTimeout(TIMEOUT);
            System.out.println("Socket has been created and timeout set!");
            //If the mode isnt octet, which is the only mode supported, stated in assignment breif.
            if (!"octet".equals(rrqPKT.getMode())) {
                //Creates error packet as the mode is not supported.
                ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.UNDEFINED, "Incorrect mode: " + rrqPKT.getMode());
                sckt.send(toDatagramPacket(errorPacket, addressOfClient, portOfClient));
                System.out.println("Only mode supported is octet! Incorrect mode found: " + rrqPKT.getMode());
                return;
            }
            //Creates the File Input Stream along with Buffer. Input stream reads the bytes.
            try (FileInputStream fileIS = new FileInputStream(rrqPKT.getFilename())) {
                System.out.println("File input stream & First buffer created!");
                byte[] firstBuffer = new byte[DATA_LEN];
                int bytesRead = fileIS.read(firstBuffer);
                System.out.println("No. of Bytes read: " + bytesRead);
                //Sets bytes read = -1, to 0, as no bytes read.
                if (bytesRead == -1) {
                    bytesRead = 0;
                }
                //Creates Data packet & Sends the packet to the client.
                DataPacket datagrampckt = new DataPacket((short) 1, firstBuffer, bytesRead);
                System.out.println("Data packet created & sent to the client!");
                clientSender((short) 1, datagrampckt, fileIS, sckt, addressOfClient, portOfClient);
                //If file not found, sends error packet!
            } catch (FileNotFoundException e) {
                System.out.println("Error Packet");
                //Error packet created and sent to the client.
                ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.FILE_NOT_FOUND, "file not found: " + rrqPKT.getFilename());
                DatagramPacket packetToSend = toDatagramPacket(errorPacket, addressOfClient, portOfClient);
                //Send method called.
                try {
                    sckt.send(packetToSend);
                } catch (IOException ex) {
                    //If input error occurs.
                    System.out.println("Input Error: " + e.getMessage());
                }
            } catch (IOException | TFTPException ex) {
                Logger.getLogger(RRQHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        //Catches IO and Socket Errors on intial try statement.
        } catch (SocketException ex) {
            System.out.println("Socket error has occured.\n");
        } catch (IOException ex) {
            Logger.getLogger(RRQHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Converts a Datagram Packet to TFTPPacket.
     *
     * @param datagramPckt - Datagram packet to be converted.
     * @return TFTPPacket created.
     * @throws TFTPException is thrown when packet type is unknown/not TFTPPacket.
     */
    public TFTPPacket fromDatagramPacket(DatagramPacket datagramPckt) throws TFTPException {
        return TFTPPacket.fromByteArray(datagramPckt.getData(), datagramPckt.getLength());
    }    
    
    /**
     * Converts a TFTP packet to a Datagram packet.
     *
     * @param pcktToBeConverted - TFTPPacket to be converted. (Into datagram).
     * @param serverAddress - Server's Address within packet
     * @param serversPort - Server's Port within Packet.
     * @return a Datagram packet.
     */
    public DatagramPacket toDatagramPacket(TFTPPacket pcktToBeConverted, InetAddress serverAddress, int serversPort) {
        DatagramPacket datagramPckt = new DatagramPacket(pcktToBeConverted.getPacketBytes(), 0, pcktToBeConverted.getPacketBytes().length);
        //Sets the address and the port of the packet to the server's address & port.
        datagramPckt.setAddress(serverAddress);
        datagramPckt.setPort(serversPort);
        return datagramPckt;
    }

    /**
     * Sends the Packet to the Client.
     *
     * @param initialBlockNo - 1st Block number of initial packet.
     * @param initialPacket - Initial packet to send to server.
     * @param fileIS - File input stream.
     * @param sckt - Connection socket.
     * @param address - Server's Address
     * @param port - Port of the destination.
     * @throws IOException is thrown when error occurs with fileIS.
     * @throws TFTPException is thrown when the timeout limit is reached.
     */
    public void clientSender(short initialBlockNo, TFTPPacket initialPacket, FileInputStream fileIS, DatagramSocket sckt, InetAddress address, int port) throws IOException, TFTPException {
        System.out.println("Send to Client Method Invoked!");
        //All variables to be used initialised.
        TFTPPacket sendingPacket;
        boolean firstPacketCheck = true;
        short blockNo = initialBlockNo;
        int previousPCKTLen = DATA_LEN;
        int bytesRead;
        //While max number of timeouts (15) is not reached.
        while (true) {
            //Creates Buffers.
            byte[] rcvBuffer = new byte[PCKT_LEN];
            System.out.println("Created Received Packet & File buffers ");
            byte[] fileBuffer = new byte[DATA_LEN];
            //Checks if the packet is the intial/first packet.
            if (firstPacketCheck) {
                //If it is first packet.
                sendingPacket = initialPacket;
                System.out.println("First Packet: True!");
                //If the first packet is of type DATA Packet.
                if (initialPacket instanceof DataPacket) {
                    System.out.println("First Packet is of type DATA, Packet Length Stored.");
                    //Stores the packet length.
                    previousPCKTLen = ((DataPacket) initialPacket).getPacketLength();
                }
            } else {
                //If the packet is not the first packet.
                System.out.println("First Packet: False!");
                bytesRead = fileIS.read(fileBuffer);
                System.out.println("File Input Stream Read. Bytes Read: " + bytesRead + "\n");
                if (bytesRead == -1) {
                    if (previousPCKTLen == DATA_LEN) {
                        //If the packet length is equal to the data length. I.e 512 Final PCKT.
                        bytesRead = 0;
                    } else {
                        break;
                    }
                }
                //Packet to be sent created as a Data Packet.
                sendingPacket = new DataPacket(blockNo, fileBuffer, bytesRead);
                System.out.println("Created packet to be send!");
                //Previous Packet length will be equal to the bytes Read. As final packet.
                previousPCKTLen = bytesRead;

            }
            //Initialises the amount of current timeouts.
            int timeouts = 0;
            //Creates Datagram Packet using received packet buffer & its length.
            DatagramPacket rcvDatagram = new DatagramPacket(rcvBuffer, rcvBuffer.length);
            //As long as procedure has not timed out more than 14 times. (I.E Equal/> 15).
            while (timeouts < MAX_AMOUNT_TIMEOUTS) {
                System.out.println("Datagram packet created and being sent to client with Address: " + address + "and Port: " + port);
                sckt.send(toDatagramPacket(sendingPacket, address, port));
                //Socket sent.
                System.out.println("Send method called for Socket.");
                try {
                    //Waiting and Recieved Response.
                    sckt.receive(rcvDatagram);
                } catch (SocketTimeoutException timeout) {
                    //If timeout occurs let user know & increment number of timeouts.
                    timeouts++;
                    System.out.println("Uh No! A timeout has occured!, Resending now!\n");
                    continue;
                }
                //If the block number is equal to inital block number.
                if (blockNo == initialBlockNo) {
                    //Set port to be port of the received Datagram Packet.
                    port = rcvDatagram.getPort();
                }
                //Creates a TFTP Packet from the Received datagram Packet.
                TFTPPacket receivedPckt = fromDatagramPacket(rcvDatagram);
                //If packet is of type error packet!
                if (receivedPckt instanceof ErrorPacket) {
                    //Print out error message.
                    System.out.println(((ErrorPacket) receivedPckt).getErrorMessage());
                    return;
                } else if (receivedPckt instanceof AckPacket) {
                    //Otherwise is of type Acknowledgement Packet.
                    AckPacket rcvdAck = (AckPacket) receivedPckt;
                    //Increment Block Number. Set firstpacket check to false as no longer first packet.
                    blockNo++;
                    firstPacketCheck = false;
                    break;
                }
            }
            //If 15 timeouts occur, (MAX is 15), throw exception.
            if (timeouts == MAX_AMOUNT_TIMEOUTS) {
                throw new TFTPException("Timeout limit  of 15 has been reached!\n");
            }
        }
    }

}

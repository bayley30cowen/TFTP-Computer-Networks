package tftp.udp.server;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import tftp.udp.server.PacketClasses.*;

/**
 * @author 184514
 * @date 04/05/2020
 */
public class WRQHandler implements Runnable{

    private final int portClient;
    private final InetAddress addressClient;
    WRQPacket wrqPKT;
    //Constants, Timeout value random number in ms.
    private final int PCKT_LEN = 516, DATA_LEN = 512, MAX_AMOUNT_TIMEOUTS = 15, TIMEOUT = 7000;

    /**
     * Constructor creates  a new Write Request (WRQ) handler.
     * 
     * @param portClient - Port used in order to connect to client.
     * @param adressClient - Client's Address
     * @param wrqpckt - Packet to be sent from the client.
     */
    public WRQHandler(int portClient, InetAddress adressClient, WRQPacket wrqpckt) {
        this.addressClient = adressClient;
        this.portClient = portClient;
        this.wrqPKT = wrqpckt;
    }
    
    /**
     * This method is ran when a Thread starts. Will continue to takes commands until exit is called.
     */
    @Override
    public void run() {
        System.out.println("A Write Request (WRQ) has been received: " + wrqPKT + "from the address: " + addressClient + " with the port: " + portClient);
        try {
            DatagramSocket socket = new DatagramSocket();
            System.out.println("Datagram Socket has been created with a timeout of: " + TIMEOUT + "ms");
            socket.setSoTimeout(TIMEOUT);
            //If mode of the Write Request Packet is not octet (only mode supported).
            if(!"octet".equals(wrqPKT.getMode())){
                //Create an error packet
                ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.UNDEFINED, "Only Octet is supported! Invalid Mode: " + wrqPKT.getMode());
                //Socket sent with error packet to client.
                socket.send(toDatagramPacket(errorPacket, addressClient, portClient));
                System.out.println("Error Packet Sent! Invalid Mode used: " + wrqPKT.getMode());   
            }
            try (FileOutputStream fileOS = new FileOutputStream(wrqPKT.getFilename())){
                //File output stream is created and recieve file has been called.
                System.out.println("File output stream created & receive file method is called!");
                fileReceiver(socket, new AckPacket(0), addressClient, portClient, fileOS);       
            }
            catch (FileNotFoundException fnfe){
                //If the file is not found, Error packet created and socket sent using send method.
                ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.FILE_NOT_FOUND, "Unable to write to file: " + wrqPKT.getFilename());
                socket.send(toDatagramPacket(errorPacket, addressClient, portClient));
            } catch (IOException ex) {
                //Further catches any IO exceptions along with TFTP exceptions.
                Logger.getLogger(WRQHandler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TFTPException ex) {
                Logger.getLogger(WRQHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        } catch (SocketException ex) {
            //If a socket exception has occured, let user know.
            System.out.println("Receive has failed: " + ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(WRQHandler.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    /**
     * Writes file to the server.
     *
     * @param sckt - Socket to enable connection to server.
     * @param initialPckt - First packet to send to the server.
     * @param serverAddress - Server's address
     * @param port - Port of destination.
     * @param fileOS - File output-stream used to write to the server/destination.
     * @throws TFTPException is thrown when the timeout limit (15) is reached.
     */
    public void fileReceiver(DatagramSocket sckt, TFTPPacket initialPckt, InetAddress serverAddress, int port, FileOutputStream fileOS) throws TFTPException {
        //Creates the received buffer (byte array) and creates the Datagram Packet using the buffer.
        byte[] rcvBuffer = new byte[PCKT_LEN];
        DatagramPacket rcvDataPckt = new DatagramPacket(rcvBuffer, rcvBuffer.length);
        //Creates boolean to check if packet is the inital Packet
        boolean checkFirstPckt = true;
        //Creates the packet to send of type TFTP.
        TFTPPacket packetToSend;
        //Sets the number of noOfTimeouts to 0 and the number of acknowledgements to 0.
        short ackNo = 0, noOfTimeouts = 0;
        //While timeout limit (15) is not reached.
        while (true) {
            //If packet is the initial/first packet.
            if (checkFirstPckt) {
                //Packet to send set to the initial packet.
                packetToSend = initialPckt;
            } else {
                //Otherwise sets the packet to send as a new ACK packet.
                packetToSend = new AckPacket(ackNo);
            }
            //While timeout limit (15) is not reached.
            while (noOfTimeouts < MAX_AMOUNT_TIMEOUTS) {
                try {
                    //datagram Packet is sent by the socket.
                    sckt.send(toDatagramPacket(packetToSend, serverAddress, port));
                    System.out.println("Datagram Packet sent by the socket! (Send method invoked).");
                    try {
                        //Waits to receive a response.
                        sckt.receive(rcvDataPckt);
                        System.out.println("Socket has responded! Datagram Packet received.");
                    } catch (SocketTimeoutException timeout) {
                        //If a timeout occurs increment number of current of timeouts. Let user know.
                        ++noOfTimeouts;
                        System.out.println("Oh oh! A timeout has occured!, Resending packet now!\n");
                        continue;
                    }
                    //No timeout has occured. If ackNo is still 0, get the port of the received data packet.
                    if (ackNo == 0) {
                        port = rcvDataPckt.getPort();
                        System.out.println("Port got: " + port);
                    }
                    //Creates a new TFTP packet. Creates it from Received datagram packet.
                    TFTPPacket tftpPckt;
                    tftpPckt = fromDatagramPacket(rcvDataPckt);
                    //If packet is of type ERROR.
                    if (tftpPckt instanceof ErrorPacket) {
                        //Print the error & its error message to the user.
                        System.out.println(((ErrorPacket) tftpPckt).getErrorMessage());
                        return;
                    } else if (tftpPckt instanceof DataPacket) {
                        //Otherwise if its of type Data.
                        System.out.println("Packet received & is a Data Packet!");
                        //Create a data packet by converting tftp pckt.
                        DataPacket dataPckt = (DataPacket) tftpPckt;
                        //Prints the length and block number & ack number of the data packet.
                        System.out.println("Data Packet of length: " + dataPckt.getPacketLength());
                        System.out.println("Block number of Data packet: " + dataPckt.getBlockNumber() + " with ACK number of: " + ackNo);
                        //If the block number of the data packet is equal to one more than the acknowledgment number.
                        if (dataPckt.getBlockNumber() == (ackNo + 1)) {
                            //Writes to the data packet using file output stream, and increases ack number.
                            fileOS.write(dataPckt.getPacketBytes(), DataPacket.DATA_OFFSET, dataPckt.getPacketLength());
                            ackNo++;
                            //Can no longer be first packet.
                            checkFirstPckt = false;
                            //If data packet is the final packet.
                            if (dataPckt.isFinalPacket()) {
                                System.out.println("Data Packet is the final packet! New Ack packed creatred to send!");
                                packetToSend = new AckPacket(ackNo);
                                sckt.send(toDatagramPacket(packetToSend, serverAddress, port));
                                System.out.println("AckPacket has been sent!\n");
                                return;
                            }
                            break;
                        }
                    }
                //If IO Exception occurs, Error packet creatred and prints Error message.
                } catch (IOException e) {
                    ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.UNDEFINED, "Packet Invalid.");
                    System.out.println(errorPacket.getErrorMessage());
                }
            }
            //If timeout limnit reached, let user know and throw exception.
            if (noOfTimeouts == MAX_AMOUNT_TIMEOUTS) {
                throw new TFTPException("Timeout limit of 15 has been reached!!\n");
            }
        }
    }
    
    /**
     * Converts A Datagram packet to A TFTPPacket.
     * 
     * @param dataPckt - Datagram packet to be converted.
     * @return TFTPPacket created.
     * @throws TFTPException is thrown when packet type is not TFTP.
     */
    public TFTPPacket fromDatagramPacket(DatagramPacket dataPckt) throws TFTPException {
        return TFTPPacket.fromByteArray(dataPckt.getData(), dataPckt.getLength());
    }    
    
    /**
     * Converts A TFTP packet to  a Datagram Packet.
     * 
     * @param tftpPckt - TFTPPacket to be converted.
     * @param serversAddress - Server's Address within the packet.
     * @param port - Server's port within the packet.
     * @return Datagram packet created from TFTPPacket
     */
    public DatagramPacket toDatagramPacket(TFTPPacket tftpPckt, InetAddress serversAddress, int port) {
        DatagramPacket datagramPckt = new DatagramPacket(tftpPckt.getPacketBytes(), 0, tftpPckt.getPacketBytes().length);
        //Converts the packet and sets the port & address to the servers wihtin the packet.
        datagramPckt.setPort(port);
        datagramPckt.setAddress(serversAddress);
        return datagramPckt;
    }   
}

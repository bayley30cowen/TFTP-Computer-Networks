package tftp.udp.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Scanner;

import tftp.udp.client.PacketClasses.*;

/**
 * @author 184514
 * @date 29/04/2020
 */
public class TFTPUDPClient extends Thread {

    private int serversPort;
    //Imported through Java.net
    InetAddress serversAddress;
    //Constants Packet Length set to 516, Data Length set to 512 and the max amount of timeouts set to 15 (Random number chosen)
    private final int PCKT_LEN = 516, DATA_LEN = 512, MAX_AMOUNT_TIMEOUTS = 15;
    //Random number again chosen for timeout length
    int TIMEOUT = 7000;

    /**
     * Constructor makes a TFTPUDPClient with the port.
     *
     * @param port - default port to connect to
     */
    public TFTPUDPClient(int port) {
        this.serversPort = port;
    }

    /**
     * Grabs the file from server.
     *
     * @param serverFile - server file path
     * @param localFile - local file path
     * @throws java.io.IOException when connection failure occurs
     * @throws TFTPException only thrown when the packet is incorrectly padded
     */
    public void grabFile(String serverFile, String localFile) throws SocketException, IOException, TFTPException {
        DatagramSocket sckt = new DatagramSocket();
        //Enables SO_TIMEOUT with the specified timeout, (7000) in milliseconds.
        sckt.setSoTimeout(TIMEOUT);
        try {
            FileOutputStream fileOS = new FileOutputStream(localFile);
            receiveFile(sckt, new RRQPacket(serverFile), serversAddress, serversPort, fileOS);
        //if the file isnt found, thrown the file not found exception, along with creating an error packet.
        } catch (FileNotFoundException ex) {
            ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.FILE_NOT_FOUND, "Can't write to: " + localFile);
            //.send called sending error packet
            sckt.send(toDatagramPacket(errorPacket, serversAddress, serversPort));
        }

    }

    /**
     * Receives the file from server.
     *
     * @param sckt - Socket to connect to server
     * @param initialPacket - First packet to send to server
     * @param serverAdress - Server serverAdress
     * @param port - Destination
     * @param fileos - File output stream used to write to the server
     * @throws TFTPException thrown when timeout limit reached (7000)
     */
    public void receiveFile(DatagramSocket sckt, TFTPPacket initialPacket, InetAddress serverAdress, int port, FileOutputStream fileOS) throws TFTPException {
        System.out.println("***Receive file***");
        System.out.println("Buffer created.");
        System.out.println("Datagram created.");
        boolean isInitial = true;
        TFTPPacket packetToSend;
        short ackNum = 0, timeouts = 0;
        //Always executes this until timeout limit reached.
        while (true) {
            //Byte Array (Buffer) set to size of packet length (516). Buffer Created.
            byte[] receiveBuffer = new byte[PCKT_LEN];
            //Datagram Packet Created.
            DatagramPacket rcvDatagram = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            //If the packet is the first (initial)
            if (isInitial) {
                System.out.println("First packet!");
                packetToSend = initialPacket;
            } else {
                System.out.println("Not the first packet! ");
                //If not the first packet, an acknowledgment packet is created to be sent.
                packetToSend = new AckPacket(ackNum);
            }
            //As long as number of timeouts is less than the maximum (15)
            while (timeouts < MAX_AMOUNT_TIMEOUTS) {
                try {
                    //Datagram sent by the socket and waits for response.
                    sckt.send(toDatagramPacket(packetToSend, serverAdress, port));
                    System.out.println("Datagram sent by the socket.");
                    try {
                        System.out.println("Waiting to receive response");
                        sckt.receive(rcvDatagram);
                        System.out.println("Socket Received Datagram!");
                    } catch (SocketTimeoutException timeout) {
                        ++timeouts;
                        /*If timeout occurs, attempt to resend the packet, 
                        while incrementing number of timeouts. */
                        System.out.println("Timeout occured!, Now resending the packet.");
                        continue;
                    }
                    System.out.println("No Timeout has occurred.");
                    if (ackNum == 0) {
                        //If ackNum is still 0, get the port of datagram.
                        port = rcvDatagram.getPort();
                        System.out.println("Got the port: " + port);
                    }
                    //Creates a TFTP Packet named pkt.
                    TFTPPacket pkt;
                    pkt = fromDatagramPacket(rcvDatagram);
                    //If the packet is an error packet, prints error message!
                    if (pkt instanceof ErrorPacket) {
                        System.out.println(((ErrorPacket) pkt).getErrorMessage());
                        return;
                    } else if (pkt instanceof DataPacket) {
                        //Else if the packet received is a data packet (What we want.)
                        System.out.println("Data Packet has been received!!");
                        //Creates a data packet.
                        DataPacket datapkt = (DataPacket) pkt;
                        if (datapkt.getBlockNumber() == (ackNum + 1)) {
                            System.out.println("Writing to the Data Packet.");
                            fileOS.write(datapkt.getPacketBytes(), DataPacket.DATA_OFFSET, datapkt.getPacketLength());
                            System.out.println("Acknowledgment number incremented.");
                            ackNum++;
                            //As first packet has been received, following can no longer be initial packet.
                            isInitial = false;
                            //Checks if the data packet is the final packet (less than 512).
                            if (datapkt.isFinalPacket()) {
                                System.out.println("Data Packet is Final packet!");
                                packetToSend = new AckPacket(ackNum);
                                System.out.println("Created a New packet to send.");
                                sckt.send(toDatagramPacket(packetToSend, serverAdress, port));
                                System.out.println("Send method called! Sending AckPacket.");
                                return;
                            }
                            break;
                        }
                    }
                } catch (IOException e) {
                    //If IOException, send error packet.
                    ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.UNDEFINED, "Invalid Packet.");
                    //Prints the error message, so the error can be known.
                    System.out.println(errorPacket.getErrorMessage());
                }
            }
            //If the maximum amount of timeouts is reached.
            if (timeouts == MAX_AMOUNT_TIMEOUTS) {
                //Prints limit reached message.
                System.out.println("Timeout limit has been reached.");
                //Breaks about of while(true) loop.
                return;
            }
        }

    }

    /**
     * Sends the file to the server.
     *
     * @param localFile - file to send from the clients side.
     * @param serverFile - File name to be stored on servers side.
     * @throws SocketException is thrown when a socket error occurs
     * @throws IOException is thrown when an error occurs with the fis.
     * @throws TFTPException is thrown when an error occurs when sending the file to the server.
     */
    public void sendFileToServer(String localFile, String serverFile) throws SocketException, IOException, TFTPException {
        //Creates a socket (sckt) and sets the timeout to 7000ms
        DatagramSocket sckt = new DatagramSocket();
        sckt.setSoTimeout(TIMEOUT);
        try (FileInputStream fis = new FileInputStream(localFile)) {
            //Sends to the server a new Write Request packet.
            sendToServer((short) 0, new WRQPacket(serverFile), fis, sckt, serversAddress, serversPort);
        } catch (FileNotFoundException ex) {
            //Error packet sent if the file cannot be found, along with the error message.
            ErrorPacket errorPacket = new ErrorPacket(ErrorPacket.ErrorCodes.FILE_NOT_FOUND, "File " + localFile + " cannot be found!");
            System.out.println(errorPacket.getErrorMessage());
        }
    }

    /**
     * Converts a Datagram packet to a TFTPPacket.
     *
     * @param datagramPckt - Datagram packet to be converted.
     * @return TFTPPacket Created from datagramPacket
     * @throws TFTPException Thrown when packet type unknown
     */
    public static TFTPPacket fromDatagramPacket(DatagramPacket datagramPckt) throws TFTPException {
        return TFTPPacket.fromByteArray(datagramPckt.getData(), datagramPckt.getLength());
    }    
    
    /**
     * Converts a TFTP packet to a Datagram packet.
     *
     * @param pckt - The TFTPPacket which will be converted.
     * @param serverAddress - Server's Address in the packet
     * @param port - Server's port in packet
     * @return A Datagram packet (datagramPckt) created from the TFTPPacket
     */
    public static DatagramPacket toDatagramPacket(TFTPPacket pckt, InetAddress serverAddress, int port) {
        DatagramPacket datagramPckt = new DatagramPacket(pckt.getPacketBytes(), 0, pckt.getPacketBytes().length);
        //Sets the address and port in the packet.
        datagramPckt.setAddress(serverAddress);
        datagramPckt.setPort(port);
        return datagramPckt;
    }

    /**
     * Sends the packet to the server.
     *
     * @param firstPacketBlockNo - The block number of first packet
     * @param initialPacket - The first packet to send to server
     * @param fileis - The File input stream to read from.
     * @param sckt- The socket that will be used to connect to server
     * @param address - Server Address
     * @param port - port of destination
     * @throws IOException is thrown when error occurs with the fileis
     * @throws TFTPException is thrown when timeout limit reached (15 timeouts)
     */
    public void sendToServer(short firstPacketBlockNo, TFTPPacket initialPacket, FileInputStream fileis, DatagramSocket sckt, InetAddress address, int port) throws IOException, TFTPException {
        //Packet to send is created, block number set to the initial packets block number.
        TFTPPacket pcktToSend;
        boolean isInitialPckt = true;
        short blockNo = firstPacketBlockNo;
        int previousPcktLen = DATA_LEN;
        int bytesRead;
        //While not timedout.
        while (true) {
            //Buffers created using constants.
            byte[] rcvBuffer = new byte[PCKT_LEN];
            byte[] fileBuffer = new byte[DATA_LEN];
            FileInputStream fileIS = fileis;
            //If the packet is the first packet. Send this to be the packet to send.
            if (isInitialPckt) {
                System.out.println("First Packet!");
                pcktToSend = initialPacket;
                //If the initial packet is a data packet, sets the previous pckt length to this packets length.
                if (initialPacket instanceof DataPacket) {
                    previousPcktLen = ((DataPacket) initialPacket).getPacketLength();
                }
            } else {
                //Else reads the file buffer and prints the bytes read.
                System.out.println("Reading the file buffer.");
                bytesRead = fileIS.read(fileBuffer);
                System.out.println("Bytes read from the file buffer: " + bytesRead);
                //If the bytes read returns -1, prints previous pckt lenghth and data length.
                if (bytesRead == -1) {
                    System.out.println("Prevoius Packet Length: " + previousPcktLen);
                    System.out.println("Data Length: " + DATA_LEN);
                    if (previousPcktLen == DATA_LEN) {
                        //if they are equal sets bytes read to 0, as none read.
                        bytesRead = 0;
                    } else {
                        //otherwise exits while(true)
                        break;
                    }
                }
                //Prints the block number and bytes read, as used to create new data packet to send.
                System.out.println("Block Number: " + blockNo + " bytes read: " + bytesRead + " Data Packet Created.");
                pcktToSend = new DataPacket(blockNo, fileBuffer, bytesRead);
                DataPacket dataPacket = (DataPacket) pcktToSend;
                previousPcktLen = bytesRead;
            }
            //Initialises the amount of current timeouts to be compared with max amount.
            int timeouts = 0;
            //Creates a new Datagram packet using the receive buffer and its length.
            DatagramPacket rcvDatagram = new DatagramPacket(rcvBuffer, rcvBuffer.length);
            System.out.println("New datagram packet created");
            //While number ofm timeouts is less than 15.
            while (timeouts < MAX_AMOUNT_TIMEOUTS) {
                //Prints the number of current timeouts and calls the send method.
                System.out.println("Number of timeouts: " + timeouts);
                sckt.send(toDatagramPacket(pcktToSend, address, port));
                System.out.println("Method send called!");
                //Waits for and receives response.
                try {
                    System.out.println("Waiting for a response.");
                    sckt.receive(rcvDatagram);
                    System.out.println("Response received!");
                } catch (SocketTimeoutException timeout) {
                    //If a timeout occurs, increments number of timeouts.
                    System.out.println("A Timeout has occured! Now resending!");
                    timeouts++;
                    continue;
                }
                //If the blockNo is the same as the first packet's block number, print the Port.
                if (blockNo == firstPacketBlockNo) {
                    port = rcvDatagram.getPort();
                    System.out.println("Block Number equal to the first packet's block number!");
                    System.out.println("The port: " + port);
                }
                //Creatres new TFTP Packet from the Datagram.
                TFTPPacket rcvPackt = fromDatagramPacket(rcvDatagram);
                System.out.println("A TFTPPacket created using the Datagram Packet.");
                //If of packet type = Error, Creates Error packet & prints the error message.
                if (rcvPackt instanceof ErrorPacket) {
                    System.out.println(((ErrorPacket) rcvPackt).getErrorMessage());
                    return;
                //Otherwise, if its of type = ACK, Creates ACK Packet & prints Block number.
                } else if (rcvPackt instanceof AckPacket) {
                    System.out.println("Packet is of type: ACK");
                    AckPacket rcvdAck = (AckPacket) rcvPackt;
                    System.out.println("Block number is: " + blockNo);
                    System.out.println("Ack Packet block number is: " + rcvdAck.getBlockNo());
                    //No more packets can now be the intial packet.
                    isInitialPckt = false;
                    blockNo++;
                    break;
                }
            }
            //If the number of timeouts has hit the max.
            if (timeouts == MAX_AMOUNT_TIMEOUTS) {
                System.out.println("Timeout limit (15) has been reached!");
                return;
            }

        }
    }
    /**
     * Called to connect to the server.
     *
     * @param args - This is the user's input which should consist of the host-name & port.
     * 
     * @throws UnkownHostException is thrown when the hostname is not recognised.
     * @throws NumberFormatException is thrown when the port is not a number.
     */
    public void connect(String[] args) {
        if (args.length == 1) {
            System.out.println("Connect [host-name] [port]");
            return;
        }
        if (args.length >= 2) {
            try {
                serversAddress = InetAddress.getByName(args[1]);
            } catch (UnknownHostException ex) {
                System.out.println("Host " + args[1] + " is Unknown!");
            }
        }

        if (args.length >= 3) {
            try {
                serversPort = Integer.parseInt(args[2]);
            } catch (NumberFormatException nfe) {
                System.out.println("Port value: " + args[2] + " is Invalid!");
            }
        }
    }
    
    /**
     * Method is ran when a thread is started. Will takes user inputs (commands) until exit (quit) is called.
     */
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        //Lets the user know the command help shows a list of all the commands.
        System.out.println("***************************************");
        System.out.println("******* TFTP UDP Client Started *******");
        System.out.println("***************************************");
        System.out.println("Enter 'help' for a list of the available commands!");
        //Until quit command is called.
        while (true) {
            //used as a splitter for readability.
            System.out.println(">>");
            String line = scanner.nextLine();
            String[] args = line.split(" ");
            if (args.length >= 1) {
                switch (args[0]) {
                    //Command connect calls connect method, to connect to server.
                    case "connect":
                        connect(args);
                        break;
                    //Command 1 is used to move a file from the server (host) to the client.
                    case "1":
                        String fileOnServer, localFile;
                        if (args.length == 1) {
                            System.out.println("Reading from remote path!");
                            return;
                        }
                        //if connect command has not been called yet!
                        if (serversAddress == null) {
                            System.out.println("To use this command you must first connect to a Server!");
                        }
                        //File is the second input from the user, the input after the command.
                        fileOnServer = args[1];
                        if (args.length >= 3) {
                            localFile = args[2];
                        } else {
                            localFile = Paths.get(fileOnServer).getFileName().toString();
                        }
                         {
                            try {
                                //Try to grab the file from the server.
                                grabFile(fileOnServer, localFile);
                            } catch (SocketException ex) {
                                Logger.getLogger(TFTPUDPClient.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(TFTPUDPClient.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (TFTPException ex) {
                                Logger.getLogger(TFTPUDPClient.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                    //Command 2 copies a file from the client to the server.
                    case "2":
                        if (args.length == 1) {
                            System.out.println("Writing to the remote-path!");
                            return;
                        }
                        //if connect command has not been called yet!
                        if (serversAddress == null) {
                            System.out.println("To use this command you must first connect to a Server!");
                        }
                        localFile = args[1];
                        if (args.length >= 3) {
                            fileOnServer = args[2];
                        } else {
                            fileOnServer = Paths.get(localFile).getFileName().toString();
                        }
                         {
                            try {
                                //Try to send to local file to the host.
                                sendFileToServer(localFile, fileOnServer);
                            } catch (IOException | TFTPException ex) {
                                Logger.getLogger(TFTPUDPClient.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                    case "t":
                        if (args.length == 1) {
                            System.out.println("Timeout time in ms: " + TIMEOUT);
                            return;
                        }
                        try {
                            //If the value entered isnt an integer value.
                            TIMEOUT = Integer.parseInt(args[1]);
                            System.out.println("Timeout time in ms: " + TIMEOUT);
                        } catch (NumberFormatException nfe) {
                            System.out.println("Invalid timeout value: " + args[1]);
                        }
                        break;
                    //Breaks out of the loop, quits thge process.
                    case "quit":
                        return;
                    //Command help prints a list of commands for the user if they need help.
                    case "help":
                        System.out.println("\n*************************************\n*********** Command List ************\n*************************************");
                        System.out.println("connect - connect to server:  [host-name] [port]");
                        System.out.println("1 - Get file from remote-path to local-path!");
                        System.out.println("2 - Put file from local-path to remote-path!");
                        System.out.println("t - Timeout value (ms)");
                        System.out.println("quit - exit");
                        System.out.println("*************************************");
                        break;
                    //If an unrecognized command is entered
                    default:
                        System.out.println("Unrecognised command! Enter 'help' for list of commands!");
                        break;

                }
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //Port was a random number above 1024.
        Thread udpClient = new TFTPUDPClient(8451);
        udpClient.start();
    }

}

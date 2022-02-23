package tftp.udp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tftp.udp.server.PacketClasses.*;

/**
 * @author 184514
 * @date 02/05/2020
 */
public class TFTPUDPServer extends Thread {

    private final int port;
    private final ExecutorService executor;
    private final int PCKT_LEN = 516;

    /**
     * Constructor initialises a  new TFTP UDP Server.
     * @param port - port given by user, which is used to connect to.
     */
    public TFTPUDPServer(int p){
        this.port = p;
        this.executor = Executors.newCachedThreadPool();
    }

    
    /**
     * This method is ran when a Thread is started. 
     * Will continue to takes commands until exit is called.
     */
    @Override
    public void run() {
        try {
            //Socket created using port supplied.
            DatagramSocket sckt = new DatagramSocket(port);
            System.out.println("******************************************\n*********** TFTP UDP Server **************\n******************************************");    
            System.out.println("Socket, Buffer & Packet created with port: " + port);
            byte[] buffer = new byte[PCKT_LEN];
            //packet created using buffer and its length.
            DatagramPacket rcvdPacket = new DatagramPacket(buffer, buffer.length);
            while(true){
                try{
                   sckt.receive(rcvdPacket); 
                   System.out.println("Packet has been received!");
                }
                catch (IOException e){
                    //If packet isnt received / error occurs.
                    System.out.println("Error occurred with receiving the packet!!");
                    continue;
                }
                //Creates a TFTP Packet using Datagram Packet.
                TFTPPacket pckt = fromDatagramPacket(rcvdPacket);
                //Depending on the type of packet received.
                switch(pckt.getPacketType()){
                    case READ:
                        System.out.println("Calling Read Request Handler (RRQ) & Read request response has been created.");
                        executor.submit(new RRQHandler(rcvdPacket.getPort(), rcvdPacket.getAddress(), (RRQPacket) pckt));
                        break;
                    case WRITE:
                        System.out.println("Calling Write Request Handler (WRQ) & Write Request response has been created.");
                        executor.submit(new WRQHandler(rcvdPacket.getPort(), rcvdPacket.getAddress(), (WRQPacket) pckt));
                        break;
                    default:
                        //If type is neither a read or write request, will be ignored.
                        System.out.println("Ignoring the packet: " + pckt + " As not of type Read or Write.");
                        break;
                }
            }
        } catch (SocketException ex) {
            System.out.println("Socket Error has occured.");
        } catch (TFTPException ex) {
            Logger.getLogger(TFTPUDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Converts A Datagram packet to TFTP packet.
     * 
     * @param datapckt - Datagram packet to be converted.
     * @return TFTPPacket created from datagram packet
     * @throws TFTPException is thrown when the type of packet is not a TFTP Packet.
     */
    public TFTPPacket fromDatagramPacket(DatagramPacket datapckt) throws TFTPException{
        return TFTPPacket.fromByteArray(datapckt.getData(), datapckt.getLength());
    }
    
    /**
     * Main Method.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //Port was a random number greater than 1024.
        int defaultPort = 8451;
        int port = defaultPort;
        for (int i = 0; i < args.length - 1;i++){
            if(args[i].equals("-port")){
                port = Integer.parseInt(args[i+1]);
            }
        }
        TFTPUDPServer host = new TFTPUDPServer(port);
        host.start();
    }
    
}

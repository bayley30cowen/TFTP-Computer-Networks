package tftp.udp.client;

/**
 * @author 184514
 */
public class TFTPException extends Exception{

    /**
     * Creates a TDTPException.
     * 
     * @param message - error message displayed when exception is thrown
     */
    public TFTPException(String message){
        super(message);
    }
}

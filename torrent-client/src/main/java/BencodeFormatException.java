import java.rmi.server.ExportException;

public class BencodeFormatException extends ExportException {

    public BencodeFormatException(String s) {
        super(s);
    }
}

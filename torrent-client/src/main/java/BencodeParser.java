import jdk.jshell.spi.ExecutionControl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BencodeParser {

    private enum BencodeType {
        INT, STRING, LIST, DICT
    }

    PushbackInputStream in;

    // Test with a simple torrent file
    public static void main(String[] args) {
        try {
            InputStream in = BencodeParser.class.getResourceAsStream("debian.torrent");
            assert in != null;

            BencodeParser b = new BencodeParser(in);
            Map<String, Object> d = b.getDict();
            System.out.println(d.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a BencodeParser from an InputStream.
     * @param in an InputStream.
     */
    public BencodeParser(InputStream in) {
        this.in = new PushbackInputStream(in, 1);
    }

    /**
     * Return current byte in BencodeParser input stream without reading it.
     * @return An int representing the next byte in the InputStream.
     */
    private int peek() throws IOException {
        int val = in.read();
        if (val == -1)
            throw new EOFException();

        in.unread(val);
        return val;
    }

    /**
     * Reads the next character and asserts that it matches the specified
     * BencodeType.
     * @param type the BencodeType to check.
     * @throws IOException if the input stream is closed, or some other IO error
     * occurs.
     */
    public void assertType(BencodeType type) throws IOException {
        int t = peek();

        switch(t) {
            case 'i': if (type == BencodeType.INT) {in.read(); return;}
            case 'l': if (type == BencodeType.LIST) {in.read(); return;};
            case 'd': if (type == BencodeType.DICT) {in.read(); return;};
            default: if (type == BencodeType.STRING) {return;}
        }

        throw new BencodeFormatException(
                String.format("'%c' does not match BencodeType %s\n", t, type));
    }

    /**
     * Returns the length of the next type.
     * @return the length of the next type.
     * @throws IOException if the input stream is closed, or some other IO error
     * occurs.
     */
    private int getLength() throws IOException {
        StringBuilder sb = new StringBuilder();
        int next = in.read();

        while (next != ':') {
            sb.append(next - '0');
            next = in.read();
        }

        if (sb.length() == 0)
            throw new BencodeFormatException("Encountered delimiter ':' before any integer\n");

        return Integer.parseInt(sb.toString());
    }

    /**
     * Return the next integer from the InputStream.
     * @return The next integer from the InputStream.
     * @throws IOException if the input stream is closed, or some other IO error
     * occurs.
     */
    public long getInt() throws IOException {
        assertType(BencodeType.INT);

        StringBuilder sb = new StringBuilder();
        int next = in.read();

        while (next != 'e') {
            sb.append(next - '0');
            next = in.read();
        }

        if (sb.length() == 0)
            throw new BencodeFormatException("Encountered delimiter 'e' before any integer\n");

        return Long.parseLong(sb.toString());
    }


    /**
     * Returns the next string from the input stream.
     * @return the next string from the input stream.
     * @throws IOException if the input stream is closed, or some other IO error
     * occurs.
     */
    public String getString() throws IOException {
        assertType(BencodeType.STRING);

        int len = getLength();
        return new String(in.readNBytes(len));
    }


    /**
     * Returns the next dictionary from the input stream.
     * @return the next dictionary from the input stream.
     * @throws IOException if the input stream is closed, or some other IO error
     * occurs.
     */
    public Map<String, Object> getDict() throws IOException {
        assertType(BencodeType.DICT);

        Map<String, Object> dict = new HashMap<>();

        // read in key-value pairs:
        while (peek() != 'e') {
            // get key:
            String key = getString();
            Object val = read();
            System.out.printf("%s: %s\n", key, val);
            dict.put(key, val);
        }

        return dict;
    }

    /**
     * Returns the next list from the input stream.
     * @return the next list from the input stream.
     * @throws IOException if the input stream is closed, or some other IO error
     * occurs.
     */
    public List<Object> getList() throws IOException {
        assertType(BencodeType.LIST);

        ArrayList<Object> l = new ArrayList<>();

        while (peek() != 'e') {
            Object val = read();
            if (val == null) {
                throw new EOFException();
            }
            l.add(val);
        }

        // skip the 'e'
        in.read();
        return l;
    }

    /**
     * Reads the next bencoded object from the input stream and returns it.
     * @return the next bencoded object from the input stream.
     * @throws IOException if the input stream is closed, or some other IO error
     * occurs.
     */
    public Object read() throws IOException {
        int typ = peek();

        if (typ == -1)
            return null;

        return switch (typ) {
            case 'i' -> getInt();
            case 'l' -> getList();
            case 'd' -> getDict();
            default -> getString();
        };
    }
}

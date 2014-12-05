package net.java.cargotracker.interfaces.handling.file;

/**
 * @author Reza Rahman
 */
public class EventLineParseException extends RuntimeException {

    private final String line;

    public EventLineParseException(String message, Throwable cause, String line) {
        super(message, cause);
        this.line = line;
    }

    public EventLineParseException(String message, String line) {
        super(message);
        this.line = line;
    }

    public String getLine() {
        return line;
    }
}

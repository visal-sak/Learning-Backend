package co.istad.visal.e_learning.expection;

/** A dependency (e.g. the AI provider) is not configured or temporarily unreachable → 503. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}

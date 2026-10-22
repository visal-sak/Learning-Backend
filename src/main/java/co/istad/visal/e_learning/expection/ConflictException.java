package co.istad.visal.e_learning.expection;

/** The request is valid but clashes with current state: duplicate name, already enrolled, category still in use. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

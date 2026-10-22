package co.istad.visal.e_learning.expection;

/** Role passed, ownership failed. Thrown by CourseAccessService. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

package co.istad.visal.e_learning.expection;

import co.istad.visal.e_learning.features.course.CourseStatus;

/** A course status move that the state machine does not allow for this role. */
public class IllegalTransitionException extends RuntimeException {
    public IllegalTransitionException(CourseStatus from, CourseStatus to) {
        super("Cannot move a course from " + from + " to " + to);
    }
}

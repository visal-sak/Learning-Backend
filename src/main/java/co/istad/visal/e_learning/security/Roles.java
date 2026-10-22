package co.istad.visal.e_learning.security;

/** Realm role names as they appear in Keycloak. Kept here so a typo is a compile error. */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String STUDENT = "STUDENT";

    private Roles() {
    }
}

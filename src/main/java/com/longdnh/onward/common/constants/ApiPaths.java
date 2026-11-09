package com.longdnh.onward.common.constants;

/**
 * Every HTTP route in one place, so a controller mapping, a security rule, a rate-limit filter and a
 * cookie path can never drift apart. Each module has a BASE and relative segments; concatenate
 * BASE + SEGMENT where an absolute path is needed (security rules, filters). Segments are compile-time
 * constants, so they are usable inside mapping annotations.
 */
public final class ApiPaths {

    public static final String API = "/api";

    private ApiPaths() {
    }

    /** Public authentication and account self-service. */
    public static final class Auth {
        public static final String BASE = API + "/auth";

        public static final String CSRF = "/csrf";
        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";
        public static final String REFRESH = "/refresh";
        public static final String LOGOUT = "/logout";
        public static final String ME = "/me";
        public static final String VERIFY_EMAIL = "/verify-email";
        public static final String RESEND_VERIFICATION = "/verification/resend";
        public static final String FORGOT_PASSWORD = "/forgot-password";
        public static final String RESET_PASSWORD = "/reset-password";
        public static final String DISABLE_ACCOUNT = "/account/disable";

        private Auth() {
        }
    }

    /** System-admin only; the whole subtree is guarded by ROLE_SYSTEM_ADMIN. */
    public static final class Admin {
        public static final String BASE = API + "/admin";
        public static final String PATTERN = BASE + "/**";

        public static final String USERS = BASE + "/users";
        public static final String USER = "/{userId}";
        public static final String USER_DISABLE = USER + "/disable";
        public static final String USER_ENABLE = USER + "/enable";
        public static final String USER_PROMOTE = USER + "/promote";

        private Admin() {
        }
    }

    /** Tenant routes; everything under an organization is scoped by its id in the path. */
    public static final class Organizations {
        public static final String BASE = API + "/organizations";
        public static final String ORGANIZATION = "/{organizationId}";

        public static final String MEMBERSHIPS = BASE + ORGANIZATION + "/memberships";
        public static final String MEMBERSHIP = "/{membershipId}";
        public static final String MEMBERSHIP_ROLE = MEMBERSHIP + "/role";
        public static final String MY_MEMBERSHIP = "/me";
        public static final String OWNERSHIP_TRANSFER = "/ownership-transfer";

        private Organizations() {
        }
    }
}

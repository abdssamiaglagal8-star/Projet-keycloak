package ma.s2m.itsp.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/** Second factor: 6-digit OTP sent by email to the already identified user. */
public final class EmailOtpAuthenticator implements Authenticator {
    private static final Logger LOG = Logger.getLogger(EmailOtpAuthenticator.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String OTP_HASH = "s2m.emailOtp.hash";
    private static final String OTP_SALT = "s2m.emailOtp.salt";
    private static final String OTP_EXPIRY = "s2m.emailOtp.expiry";
    private static final String OTP_ATTEMPTS = "s2m.emailOtp.attempts";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            LOG.warn("S2M Email OTP: no user in context (place this step AFTER Username Password)");
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            LOG.warnf("S2M Email OTP: user %s has no email", user.getId());
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    context.form()
                            .setError("emailOtpEmailMissing")
                            .createForm("email-otp.ftl"));
            return;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String code = createCode();
        String salt = randomSalt();
        long expiresAt = Instant.now().getEpochSecond() + ttl(context);
        authSession.setAuthNote(OTP_SALT, salt);
        authSession.setAuthNote(OTP_HASH, hash(salt, code));
        authSession.setAuthNote(OTP_EXPIRY, Long.toString(expiresAt));
        authSession.setAuthNote(OTP_ATTEMPTS, "0");

        try {
            // Utilisation d'une Map modifiable (HashMap) pour éviter l'UnsupportedOperationException de Keycloak
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("otp", code);
            attributes.put("ttlMinutes", Math.max(1, (ttl(context) + 59) / 60));

            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setRealm(context.getRealm())
                    .setUser(user)
                    .send("emailOtpSubject", "email-otp.ftl", attributes);

            LOG.infof("S2M Email OTP sent to user %s", user.getId());
        } catch (Exception exception) {
            LOG.errorf(exception, "S2M Email OTP: unable to send email to user %s", user.getId());
            clear(authSession);
            context.failureChallenge(
                    AuthenticationFlowError.INTERNAL_ERROR,
                    context.form()
                            .setError("emailOtpSendFailed")
                            .setAttribute("ttlSeconds", ttl(context))
                            .createForm("email-otp.ftl"));
            return;
        }

        context.challenge(
                context.form()
                        .setAttribute("ttlSeconds", ttl(context))
                        .createForm("email-otp.ftl"));
    }

 @Override
public void action(AuthenticationFlowContext context) {
    // --- Gestion du renvoi ---
    String resend = context.getHttpRequest().getDecodedFormParameters().getFirst("resend");
    if ("true".equals(resend)) {
        clear(context.getAuthenticationSession());
        authenticate(context);   // Regénère un code et renvoie l'email
        return;
    }

    // --- Validation normale du code OTP ---
    AuthenticationSessionModel authSession = context.getAuthenticationSession();
    String submittedCode = context.getHttpRequest().getDecodedFormParameters().getFirst("otp");
    if (submittedCode == null || !submittedCode.matches("\\d{6}")) {
        challengeWithError(context, "emailOtpInvalid");
        return;
    }

    long expiry = longNote(authSession, OTP_EXPIRY, 0);
    int attempts = (int) longNote(authSession, OTP_ATTEMPTS, 0) + 1;
    authSession.setAuthNote(OTP_ATTEMPTS, Integer.toString(attempts));

    if (Instant.now().getEpochSecond() > expiry) {
        clear(authSession);
        challengeWithError(context, "emailOtpExpired");
        return;
    }
    if (attempts > maxAttempts(context)) {
        clear(authSession);
        context.failureChallenge(
                AuthenticationFlowError.INVALID_CREDENTIALS,
                context.form().setError("emailOtpTooManyAttempts").createForm("email-otp.ftl"));
        return;
    }

    String salt = authSession.getAuthNote(OTP_SALT);
    String expectedHash = authSession.getAuthNote(OTP_HASH);
    if (salt == null || expectedHash == null
            || !MessageDigest.isEqual(
                    expectedHash.getBytes(StandardCharsets.UTF_8),
                    hash(salt, submittedCode).getBytes(StandardCharsets.UTF_8))) {
        challengeWithError(context, "emailOtpInvalid");
        return;
    }

    clear(authSession);
    context.success();
}

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user != null && user.getEmail() != null && !user.getEmail().isBlank();
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

    private static void challengeWithError(AuthenticationFlowContext context, String errorKey) {
        context.challenge(
                context.form()
                        .setError(errorKey)
                        .setAttribute("ttlSeconds", ttl(context))
                        .createForm("email-otp.ftl"));
    }

    private static String createCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String randomSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hash(String salt, String code) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest((salt + ':' + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static long longNote(AuthenticationSessionModel session, String key, long defaultValue) {
        try {
            return Long.parseLong(session.getAuthNote(key));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static int ttl(AuthenticationFlowContext context) {
        return configInt(context, "otpTtlSeconds", 300, 30, 3600);
    }

    private static int maxAttempts(AuthenticationFlowContext context) {
        return configInt(context, "maxAttempts", 5, 1, 20);
    }

    private static int configInt(AuthenticationFlowContext context, String name, int fallback, int min, int max) {
        try {
            if (context.getAuthenticatorConfig() == null || context.getAuthenticatorConfig().getConfig() == null) {
                return fallback;
            }
            return Math.max(min, Math.min(max, Integer.parseInt(context.getAuthenticatorConfig().getConfig().get(name))));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void clear(AuthenticationSessionModel session) {
        session.removeAuthNote(OTP_HASH);
        session.removeAuthNote(OTP_SALT);
        session.removeAuthNote(OTP_EXPIRY);
        session.removeAuthNote(OTP_ATTEMPTS);
    }
}
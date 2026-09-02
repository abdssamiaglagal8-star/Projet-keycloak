package ma.s2m.itsp.security;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/** Server-side Cloudflare Turnstile validation. */
public final class TurnstileAuthenticator implements Authenticator {
    private static final Logger LOG = Logger.getLogger(TurnstileAuthenticator.class);
    private static final Pattern SUCCESS = Pattern.compile("\\\"success\\\"\\s*:\\s*true");
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String siteKey = config(context, "siteKey");
        if (siteKey == null || siteKey.isBlank()) {
            LOG.error("Turnstile site key is not configured");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }
        context.challenge(context.form().setAttribute("turnstileSiteKey", siteKey).createForm("captcha-turnstile.ftl"));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String token = context.getHttpRequest().getDecodedFormParameters().getFirst("cf-turnstile-response");
        if (token == null || token.isBlank()) {
            fail(context, "Veuillez valider le CAPTCHA.");
            return;
        }
        String secret = config(context, "secretKey");
        if (secret == null || secret.isBlank()) {
            LOG.error("Turnstile secret key is not configured");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }

        try {
            StringBuilder payload = new StringBuilder("secret=").append(encode(secret)).append("&response=").append(encode(token));
            String remoteAddress = context.getConnection().getRemoteAddr();
            if (remoteAddress != null && !remoteAddress.isBlank()) payload.append("&remoteip=").append(encode(remoteAddress));
            HttpRequest request = HttpRequest.newBuilder(URI.create(defaultIfBlank(config(context, "verifyUrl"), "https://challenges.cloudflare.com/turnstile/v0/siteverify")))
                    .timeout(Duration.ofSeconds(timeoutSeconds(context)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2 || !SUCCESS.matcher(response.body()).find()) {
                LOG.warnf("Turnstile validation failed with HTTP status %d", response.statusCode());
                fail(context, "La vérification CAPTCHA a échoué. Réessayez.");
                return;
            }
            context.success();
        } catch (Exception exception) {
            LOG.error("Turnstile validation request failed", exception);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    @Override public boolean requiresUser() { return false; }
    @Override public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }
    @Override public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) { }
    @Override public void close() { }

    private static void fail(AuthenticationFlowContext context, String message) {
        context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                context.form().setError(message).setAttribute("turnstileSiteKey", config(context, "siteKey")).createForm("captcha-turnstile.ftl"));
    }
    private static String config(AuthenticationFlowContext context, String key) {
        return context.getAuthenticatorConfig() == null ? null : context.getAuthenticatorConfig().getConfig().get(key);
    }
    private static int timeoutSeconds(AuthenticationFlowContext context) {
        try { return Math.max(1, Math.min(30, Integer.parseInt(config(context, "timeoutSeconds")))); } catch (Exception ignored) { return 5; }
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String defaultIfBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}
package ma.s2m.itsp.security;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public final class TurnstileAuthenticatorFactory implements AuthenticatorFactory {
    public static final String ID = "s2m-turnstile";
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENTS = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED };
    private static final List<ProviderConfigProperty> CONFIG = List.of(
            property("siteKey", "Clé de site", "Clé publique Turnstile affichée dans la page de connexion.", "" , false),
            property("secretKey", "Clé secrète", "Clé secrète Turnstile. Ne jamais la commiter dans Git.", "", true),
            property("verifyUrl", "URL de vérification", "URL du service Turnstile.", "https://challenges.cloudflare.com/turnstile/v0/siteverify", false),
            property("timeoutSeconds", "Délai HTTP (secondes)", "Entre 1 et 30 secondes.", "5", false));

    @Override public String getId() { return ID; }
    @Override public String getDisplayType() { return "S2M Cloudflare Turnstile CAPTCHA"; }
    @Override public String getReferenceCategory() { return "captcha"; }
    @Override public boolean isConfigurable() { return true; }
    @Override public AuthenticationExecutionModel.Requirement[] getRequirementChoices() { return REQUIREMENTS; }
    @Override public boolean isUserSetupAllowed() { return false; }
    @Override public String getHelpText() { return "Valide Cloudflare Turnstile côté serveur."; }
    @Override public List<ProviderConfigProperty> getConfigProperties() { return CONFIG; }
    @Override public Authenticator create(KeycloakSession session) { return new TurnstileAuthenticator(); }
    @Override public void init(Config.Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
    @Override public void close() { }

    private static ProviderConfigProperty property(String name, String label, String helpText, String defaultValue, boolean secret) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name); property.setLabel(label); property.setHelpText(helpText); property.setDefaultValue(defaultValue);
        property.setType(secret ? ProviderConfigProperty.PASSWORD : ProviderConfigProperty.STRING_TYPE);
        return property;
    }
}
package ma.s2m.itsp.security;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public final class EmailOtpAuthenticatorFactory implements AuthenticatorFactory {
    public static final String ID = "s2m-email-otp";
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENTS = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED };
    private static final List<ProviderConfigProperty> CONFIG = List.of(
            property("otpTtlSeconds", "Durée de validité (secondes)", "Entre 30 et 3600 secondes.", "300"),
            property("maxAttempts", "Nombre maximal d'essais", "Entre 1 et 20 essais.", "5"));
    private static final Authenticator INSTANCE = new EmailOtpAuthenticator();

    @Override public String getId() { return ID; }
    @Override public String getDisplayType() { return "S2M Email OTP"; }
    @Override public String getReferenceCategory() { return "otp"; }
    @Override public boolean isConfigurable() { return true; }
    @Override public AuthenticationExecutionModel.Requirement[] getRequirementChoices() { return REQUIREMENTS; }
    @Override public boolean isUserSetupAllowed() { return false; }
    @Override public String getHelpText() { return "Envoie un code OTP à six chiffres par e-mail."; }
    @Override public List<ProviderConfigProperty> getConfigProperties() { return CONFIG; }
    @Override public Authenticator create(KeycloakSession session) { return INSTANCE; }
    @Override public void init(Config.Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
    @Override public void close() { }

    private static ProviderConfigProperty property(String name, String label, String helpText, String defaultValue) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name); property.setLabel(label); property.setHelpText(helpText);
        property.setType(ProviderConfigProperty.STRING_TYPE); property.setDefaultValue(defaultValue);
        return property;
    }
}

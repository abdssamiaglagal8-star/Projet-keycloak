# keycloak-security-extension

Extension Keycloak 26.7 pour un second facteur OTP par e-mail et la validation Cloudflare Turnstile.

## Build

```bash
mvn -s settings-local.xml clean package
```

Le build Maven standard produit `target/keycloak-security-extension.jar`. Dans cette copie restaurée, un dossier invalide porte déjà ce nom ; le JAR vérifié est donc disponible dans `dist/keycloak-security-extension.jar` et Docker Compose le monte automatiquement. Redémarrez ensuite Keycloak :

```bash
docker-compose restart keycloak
```

## Providers

| Provider ID | Rôle |
| --- | --- |
| `s2m-turnstile` | Vérification Turnstile côté serveur. |
| `s2m-email-otp` | Code à six chiffres envoyé par e-mail. |

Ajoutez les exécutions dans un flow dérivé de `browser` dans cet ordre : Turnstile, Username/Password, Email OTP.

Les formulaires `captcha-turnstile.ftl` et `email-otp.ftl` doivent être fournis par le thème de login actif. La clé secrète Turnstile se configure dans Keycloak Admin ; elle ne doit pas être ajoutée au dépôt.

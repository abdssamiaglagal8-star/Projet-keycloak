# Keycloak S2M Email OTP Security Extension

Ce projet fournit une extension d'authentification à deux facteurs (2FA) par OTP envoyé par e-mail pour Keycloak.

---

## 📁 Contenu du Déploiement

Pour faire fonctionner la solution complète, seuls deux éléments compilés/configurés sont nécessaires dans le serveur Keycloak :

1. **Le module Java (Logic) :** Le fichier `keycloak-security-extension.jar` (généré dans le dossier `target/` après le build Maven).
2. **Le Thème (UI) :** Le dossier de thème contenant les gabarits de formulaires et d'e-mails (`email-otp.ftl`, etc.).

---

## 🚀 Étapes d'Installation

### 1. Copie des Fichiers dans Keycloak
* Copiez le fichier `keycloak-security-extension.jar` dans le dossier **`providers/`** de votre instance Keycloak.
* Copiez le dossier du thème dans le dossier **`themes/`** de Keycloak.

### 2. Redémarrage de Keycloak
Redémarrez l'instance Keycloak pour charger le nouveau provider :
```bash
docker compose restart keycloak
# Ou si vous utilisez l'exécutable direct :
# ./kc.sh build
# ./kc.sh start
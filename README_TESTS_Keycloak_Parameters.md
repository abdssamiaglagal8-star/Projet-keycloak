# Guide des tests — nxp-keycloak-starter & itsp-parameters-service

Document de référence pour lancer et comprendre **tous les tests** des modules :

- `nxp-keycloak-starter` — starter Keycloak (3 profils : bearer-only, public, confidential)
- `parameters` (`itsp-parameters-service`) — API métier ITSP

---

## Table des matières

1. [Prérequis](#1-prérequis)
2. [nxp-keycloak-starter — catalogue des tests](#2-nxp-keycloak-starter--catalogue-des-tests)
3. [nxp-keycloak-starter — commandes](#3-nxp-keycloak-starter--commandes)
4. [parameters — catalogue des tests](#4-parameters--catalogue-des-tests)
5. [parameters — commandes](#5-parameters--commandes)
6. [Lancer un profil Keycloak (runtime)](#6-lancer-un-profil-keycloak-runtime)
7. [Récapitulatif rapide](#7-récapitulatif-rapide)

---

## 1. Prérequis

| Outil | Usage |
|-------|--------|
| **JDK 21** | Compilation et exécution |
| **Maven 3.8+** | `mvn test` |
| **Docker** | Obligatoire pour les `*ContainerTest` (Testcontainers + Keycloak) |
| **RAM Docker** | Idéalement **6–8 Go** (surtout profil Public) |

```bash
java -version    # → 21.x
mvn -version
docker info      # Docker doit tourner
```

---

## 2. nxp-keycloak-starter — catalogue des tests

**Chemin :** `nxp-keycloak-starter/src/test/java/`

### 2.1 Tests de profils Spring (sans Docker — rapides)

| Classe | Profil | Description |
|--------|--------|-------------|
| `KeycloakBearerOnlyProfileTest` | `bearer` | Charge la config `application-bearer.yml`, vérifie `client-type=bearer-only` |
| `KeycloakPublicProfileTest` | `public` | Charge la config public + scopes openid/profile/email |
| `KeycloakConfidentialProfileTest` | `confidential` | Charge la config confidential + secret |

**Package :** `ma.s2m.nxp.common.security.keyclock`

### 2.2 Tests d’intégration Testcontainers (Docker requis)

| Classe | Profil | Méthodes principales |
|--------|--------|----------------------|
| `KeycloakBearerOnlyContainerTest` | `bearer` | `contextLoads()`, `canDecodeRealTokenFromKeycloak()`, `tokenContainsKeycloakRoles()` |
| `KeycloakPublicContainerTest` | `public` | Contexte + validations OAuth2/PKCE avec Keycloak réel |
| `KeycloakConfidentialContainerTest` | `confidential` | `hasTwoRegistrations()`, `canObtainTokenViaClientCredentials()` |

Ces tests démarrent un **Keycloak réel** via Testcontainers et importent le realm de test (`s2m-test-realm.json`).

### 2.3 Tests unitaires propriétés / config

| Classe | Package | Description |
|--------|---------|-------------|
| `KeycloackConfigurationPropertiesTest` | `...config` | Propriétés core Keycloak, issuer URL, client-type, add-on, swagger |
| `KeycloakPropertiesUnitTest` | `...keyclock` | `defaultProfileIsBearerOnly()`, `issuerUrlForKeycloak26()`, `profileSwitchingTest()` |
| `CorsConfigurationPropertiesTest` | `...cors` | Configuration CORS (`corsConfigurationSourceTest`) |
| `UserDataDtoTest` | `...dto` | DTO utilisateur (`dtoTest`) |
| `FeignClientInterceptorTest` | `...interceptor` | Intercepteur Feign JWT : sans auth, avec JWT, avec SecurityProvider |

### 2.4 Tests du provider Keycloak

| Classe | Description |
|--------|-------------|
| `KeycloakProviderTest` | `getConnectedUserTest()`, `getClaimValue()`, `getUserAttributeValues()`, `getTokenRealm()`, `getTokenString()` |
| `KeycloakProviderExempleTest` | Variantes / exemples d’usage du provider |
| `KeycloakProviderLastTest` | Cas complémentaires (claims, token string, etc.) |

### 2.5 Tests d’auto-configuration (consommateur)

| Classe | Description |
|--------|-------------|
| `KeycloakAutoConfigurationConsumerTest` | Simule un vrai projet qui consomme le starter. Nested tests : |
| └ profil `bearer` | Charge la config bearer via auto-config |
| └ profil `public` | `chargeLaConfigPublicViaAutoConfiguration()` |
| └ profil `confidential` | `chargeLaConfigConfidentialViaAutoConfiguration()` |
| └ profil `default-consumer` | `retombeSurBearerOnlyParDefaut()` |

### 2.6 Ressources de test (starter)

```
nxp-keycloak-starter/src/test/resources/
├── application-bearer.yml
├── application-public.yml
├── application-confidential.yml
├── application-default-consumer.yml
└── s2m-test-realm.json          # realm Keycloak pour Testcontainers
```

---

## 3. nxp-keycloak-starter — commandes

```bash
cd nxp-keycloak-starter
```

### Tout lancer

```bash
# Tous les tests (unitaires + profils + container si Docker OK)
mvn test
```

### Par catégorie

```bash
# --- Profils uniquement (rapide, sans Docker) ---
mvn test -Dtest=KeycloakBearerOnlyProfileTest
mvn test -Dtest=KeycloakPublicProfileTest
mvn test -Dtest=KeycloakConfidentialProfileTest
mvn test -Dtest=KeycloakBearerOnlyProfileTest,KeycloakPublicProfileTest,KeycloakConfidentialProfileTest

# --- Testcontainers (Docker obligatoire) ---
mvn test -Dtest=KeycloakBearerOnlyContainerTest
mvn test -Dtest=KeycloakPublicContainerTest
mvn test -Dtest=KeycloakConfidentialContainerTest
mvn test -Dtest="*ContainerTest"

# --- Propriétés / config ---
mvn test -Dtest=KeycloackConfigurationPropertiesTest
mvn test -Dtest=KeycloakPropertiesUnitTest
mvn test -Dtest=CorsConfigurationPropertiesTest
mvn test -Dtest=UserDataDtoTest
mvn test -Dtest=FeignClientInterceptorTest

# --- Provider ---
mvn test -Dtest=KeycloakProviderTest
mvn test -Dtest=KeycloakProviderExempleTest
mvn test -Dtest=KeycloakProviderLastTest
mvn test -Dtest="KeycloakProvider*"

# --- Auto-configuration consommateur ---
mvn test -Dtest=KeycloakAutoConfigurationConsumerTest

# --- Tout sauf les containers (si pas de Docker) ---
mvn test -Dtest='!*ContainerTest'
```

### Rapport Surefire

```bash
# Après mvn test
ls target/surefire-reports/
# Fichiers : TEST-*.xml + *.txt
```

---

## 4. parameters — catalogue des tests

**Chemin :** `parameters/src/test/java/ma/s2m/itsp/parameters/`  
**Total :** ~46 classes de test

### 4.1 Controllers (`controller/`)

| Classe | Module |
|--------|--------|
| `BinControllerTest` | BIN |
| `BinControllerWithMockitoTest` | BIN (Mockito) |
| `BinCreateControllerTest` | Création BIN |
| `CardControllerTest` | Cartes |
| `CardControllerBetaTest` | Cartes (beta) |
| `CardControllerWithMockitoTest` | Cartes (Mockito) |
| `CardProcessingControllerTest` | Processing cartes |
| `CardProcessingBetaTest` | Processing (beta) |
| `EnrollCardControllerTest` | Enrollment carte |
| `EnrollCardControllerWithMockitoTest` | Enrollment (Mockito) |
| `InstitutionControllerTest` | Institutions |
| `ProductControllerTest` | Produits |
| `TspControllerTest` | TSP |
| `TokenRequestorControllerTest` | Token Requestors |
| `DigitizationControllerTest` | Digitalisation |
| `NotificationManagementControllerTest` | Notifications |
| `ProcessingControllerTest` | Processing |

### 4.2 Services (`service/`)

| Classe | Package |
|--------|---------|
| `BinServiceTest` | `service/bin` |
| `BinServiceBetaTest` | `service/bin` |
| `CardServiceTest` | `service/card` |
| `CardFunctionnalityServiceTest` | `service/card` |
| `EnrollCardServiceTests` | `service/card/impl` |
| `InstitutionServiceTest` | `service/institution` |
| `ProductServiceTest` | `service/product` |
| `TspServiceTest` | `service/tsp` |
| `TokenRequestorServiceTest` | `service/tokenrequestor` |
| `TokenRequestorWithMockitoServiceTest` | `service/tokenrequestor` |
| `EnrollHistServiceTest` | `service/activity` |
| `DefinitionServiceTest` | `service/definition` |
| `CsvWriterTest` | `service/csvwriter` |

### 4.3 Repositories (`repository/`)

| Classe |
|--------|
| `BinRepositoryTest` |
| `CardRepositoryTest` |
| `InstitutionRepositoryTest` |
| `ProductRepositoryTest` |
| `TspRepositoryTest` |
| `TokenRequestorRepositoryTest` |
| `EnrollHistRepositoryTest` |

### 4.4 DTOs (`dto/processing/`)

| Classe |
|--------|
| `BinDTOTest` |
| `TspDTOTest` |
| `DeviceDTOTest` |
| `CreateCardDTOLazyTest` |
| `AuthenticationMethodDefinitionDTOTest` |

### 4.5 Utils / validators

| Classe | Package |
|--------|---------|
| `DateFormatServiceTest` | `utils` |
| `BeanUtilsTest` | `utils/bean` |
| `CardValidatortTest` | `utils/validator` |
| `AuthenticationMethodsValidatorTest` | `utils/validator` |

> Des builders de test existent aussi sous `utils/builder/` (helpers, pas des tests unitaires à lancer seuls).

---

## 5. parameters — commandes

```bash
cd parameters
```

### Tout lancer

```bash
mvn test
```

### Par couche

```bash
# Controllers uniquement
mvn test -Dtest='*Controller*Test'

# Services uniquement
mvn test -Dtest='*Service*Test'

# Repositories uniquement
mvn test -Dtest='*Repository*Test'

# DTOs
mvn test -Dtest='*DTO*Test'

# Utils / validators
mvn test -Dtest='*Validator*Test,*Utils*Test,DateFormatServiceTest'
```

### Par module métier

```bash
# BIN
mvn test -Dtest='Bin*Test'

# Cartes / Enrollment
mvn test -Dtest='Card*Test,Enroll*Test'

# Institutions
mvn test -Dtest='Institution*Test'

# Produits
mvn test -Dtest='Product*Test'

# TSP
mvn test -Dtest='Tsp*Test'

# Token Requestor
mvn test -Dtest='TokenRequestor*Test'

# Processing / Digitization / Notifications
mvn test -Dtest='Processing*Test,Digitization*Test,Notification*Test'
```

### Une classe précise

```bash
mvn test -Dtest=BinControllerTest
mvn test -Dtest=InstitutionServiceTest
mvn test -Dtest=KeycloakBearerOnlyContainerTest   # si lancé depuis le starter
```

### Une méthode précise

```bash
mvn test -Dtest=BinControllerTest#nomDeLaMethode
```

---

## 6. Lancer un profil Keycloak (runtime)

Utile pour **tester manuellement** l’API après les tests automatisés.

```bash
cd parameters
# Bearer
$env:ACTIVE_PROFILE="bearer"
mvn spring-boot:run

# Public
$env:ACTIVE_PROFILE="public"
mvn spring-boot:run

# Confidential
$env:ACTIVE_PROFILE="confidential"
$env:KEYCLOAK_CREDENTIALS_SECRET="ton-secret"
mvn spring-boot:run
```

### Bearer-only (défaut)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=bearer
# ou
export ACTIVE_PROFILE=bearer
mvn spring-boot:run
```

### Public

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=public
```

### Confidential (secret obligatoire)

```bash
export KEYCLOAK_CREDENTIALS_SECRET="votre-secret-client"
mvn spring-boot:run -Dspring-boot.run.profiles=confidential
```

Port par défaut du service : **9090** (`server.port` dans `application.yml`).

### Appel API avec JWT (profil bearer)

```bash
# Sans token → 401 sur routes protégées
curl -i http://localhost:9090/actuator/health

# Avec token
TOKEN="eyJ..."
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:9090/api/...
```

### Obtenir un token (exemple Client Credentials — confidential)

```bash
curl -s -X POST "http://localhost:8080/realms/s2m-test/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=s2m-app-confidential" \
  -d "client_secret=$KEYCLOAK_CREDENTIALS_SECRET" | jq -r .access_token
```

---

## 7. Récapitulatif rapide

```bash
# ========== STARTER ==========
cd nxp-keycloak-starter

mvn test                                          # tout
mvn test -Dtest='*ProfileTest'                    # 3 profils (rapide)
mvn test -Dtest='*ContainerTest'                  # intégration Keycloak (Docker)
mvn test -Dtest=KeycloakAutoConfigurationConsumerTest
mvn test -Dtest='!*ContainerTest'                 # sans Docker

# ========== PARAMETERS ==========
cd parameters

mvn test                                          # tout
mvn test -Dtest='*Controller*Test'                # controllers
mvn test -Dtest='*Service*Test'                   # services
mvn test -Dtest='*Repository*Test'                # repositories
mvn test -Dtest='Bin*Test'                         # module BIN
mvn test -Dtest='Institution*Test'                # module Institutions
```

---

## Notes importantes

1. **Profil Spring vs client-type Keycloak**  
   - Spring profile `bearer` → `keycloak.client-type: bearer-only`  
   - Spring profile `public` → `keycloak.client-type: public`  
   - Spring profile `confidential` → `keycloak.client-type: confidential`

2. **Tests Container** nécessitent Docker + image Keycloak (téléchargée automatiquement par Testcontainers).

3. **Secrets** : ne jamais committer `KEYCLOAK_CREDENTIALS_SECRET` ni les clés Turnstile. Les YML de test du starter peuvent contenir un secret de realm de **test local uniquement**.

4. **Surefire** : les rapports détaillés se trouvent dans `target/surefire-reports/` après chaque `mvn test`.

5. En cas d’échec de `KeycloakPublicContainerTest` lié à la mémoire, augmenter la RAM allouée à Docker (6–8 Go recommandés).

---

*Document généré pour le projet ITSP — nxp-keycloak-starter & itsp-parameters-service.*

# Two-Service SAML SSO Setup (Spring Boot)

This repo now contains a clean SAML-based setup:

1. `service-a` (port `9000`) - SAML Service Provider + protected API
2. `service-b` (port `9001`) - SAML Service Provider + protected API

Both services are configured as relying parties and should trust the same IdP.

## Prerequisites

1. Java 21
2. Maven Wrapper (already included in each service)
3. A SAML IdP (Keycloak/Okta/Azure AD/ADFS) with metadata URL

## Default Behavior

For local bootstrapping, each service has a classpath sample metadata file:

- `classpath:saml/idp-metadata.xml`

This is only for startup/demo wiring. For real login flow, configure real IdP metadata URLs.

## Environment Variables

PowerShell example:

```powershell
$env:SERVICE_A_SAML_IDP_METADATA_URI="https://your-idp.example.com/realms/yourrealm/protocol/saml/descriptor"
$env:SERVICE_B_SAML_IDP_METADATA_URI="https://your-idp.example.com/realms/yourrealm/protocol/saml/descriptor"
```

Use the same IdP metadata in both services for SSO.

## Run Services

Start `service-a`:

```powershell
cd service-a
.\mvnw spring-boot:run
```

Start `service-b` in a second terminal:

```powershell
cd service-b
.\mvnw spring-boot:run
```

## SP Metadata Endpoints

Expose these to your IdP app/client configuration:

1. `http://localhost:9000/saml2/service-provider-metadata/shared-idp`
2. `http://localhost:9001/saml2/service-provider-metadata/shared-idp`

## Login Flow

1. Open:
   - `http://localhost:9000/api/a/secure`
   - `http://localhost:9001/api/b/secure`
2. Anonymous request redirects to:
   - `/saml2/authenticate/shared-idp`
3. IdP login success returns to each service ACS endpoint:
   - `/login/saml2/sso/shared-idp`
4. Both services use their own session after successful SAML authentication.

## Swagger

1. Service A Swagger: `http://localhost:9000/swagger-ui.html`
2. Service B Swagger: `http://localhost:9001/swagger-ui.html`

Swagger endpoints are public; protected APIs still require SAML login/session.

## Protected APIs

1. Service A: `GET http://localhost:9000/api/a/secure`
2. Service B: `GET http://localhost:9001/api/b/secure`

Response includes subject, authorities, SAML registration id, and attribute keys.

## Test

```powershell
cd service-a
.\mvnw test

cd ..\service-b
.\mvnw test
```

# Two-Service Token Setup (JWE or Opaque)

This repo runs with 2 Spring Boot services:

1. `service-a` (`http://localhost:9000`)
   - Issues access tokens
   - Exposes JWKS public keys
   - Protects `/api/a/secure`
2. `service-b` (`http://localhost:9001`)
   - Validates `service-a` tokens
   - Protects `/api/b/secure`

No OAuth2 authorization flow is used.

## Prerequisites

1. Java 21
2. PostgreSQL

## Use .env (Windows Friendly)

1. Copy `.env.example` to `.env`
2. Edit values in `.env`
3. Run services with scripts:

```powershell
.\scripts\run-service-a.ps1
.\scripts\run-service-b.ps1
```

This loads env vars from `.env` for the current process and starts each service.

## Token Mode Switch (Mutually Exclusive)

Set both services to the same mode:

1. `jwe`
   - `service-a` issues nested signed JWT encrypted as JWE
   - `service-b` decrypts JWE, then verifies signature/issuer/audience
   - JWKS is required
2. `opaque`
   - `service-a` issues random opaque token and stores token metadata
   - `service-b` calls `service-a` introspection API for each token validation
   - JWKS is not used for request-time validation

Mode env vars:

```powershell
$env:SERVICE_A_TOKEN_MODE="jwe"   # or "opaque"
$env:SERVICE_B_TOKEN_MODE="jwe"   # or "opaque"
```

## Environment Variables

Database for `service-a`:

```powershell
$env:SERVICE_A_DB_URL="jdbc:postgresql://localhost:5432/service_a"
$env:SERVICE_A_DB_USERNAME="postgres"
$env:SERVICE_A_DB_PASSWORD="1234"
```

Issuer/audience:

```powershell
$env:SERVICE_A_ISSUER="http://localhost:9000"
$env:SERVICE_A_AUDIENCE_A="service-a"
$env:SERVICE_A_AUDIENCE_B="service-b"
$env:SERVICE_B_AUDIENCE="service-b"
```

JWE shared secret:

```powershell
$env:SERVICE_A_JWE_SECRET="change-me-jwe-secret"
$env:SERVICE_B_JWE_SECRET="change-me-jwe-secret"
```

JWT keys:

```powershell
$env:SERVICE_A_PRIVATE_KEY_PATH="D:\\keys\\private.pem"
$env:SERVICE_A_PUBLIC_KEY_PATH="D:\\keys\\public.pem"
$env:SERVICE_A_KEY_ID="key-v2"
```

Optional key rotation (old public keys):

```powershell
$env:SERVICE_A_RETIRED_PUBLIC_KEYS="key-v1=D:\\keys\\key-v1-public.pem;key-v0=D:\\keys\\key-v0-public.pem"
```

Service-to-service token client:

```powershell
$env:SERVICE_A_SERVICE_CLIENT_ID="internal-client"
$env:SERVICE_A_SERVICE_CLIENT_SECRET="internal-secret"
$env:SERVICE_A_SERVICE_CLIENT_SCOPES="service.a.read,service.b.read"
```

Opaque introspection security:

```powershell
$env:SERVICE_A_INTROSPECTION_SECRET="change-me-introspection-secret"
$env:SERVICE_B_INTROSPECTION_SECRET="change-me-introspection-secret"
$env:SERVICE_B_INTROSPECTION_URL="http://localhost:9000/api/auth/introspect"
```

Service-b strict algorithm policy:

```powershell
$env:SERVICE_B_ALLOWED_JWS_ALGORITHM="RS256"
```

## Run

Start `service-a`:

```powershell
cd service-a
.\mvnw spring-boot:run
```

Start `service-b`:

```powershell
cd service-b
.\mvnw spring-boot:run
```

## Token APIs (service-a)

1. User login token:

```powershell
curl -X POST http://localhost:9000/api/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"demo\",\"password\":\"demo1234\"}"
```

2. Service client token:

```powershell
curl -X POST http://localhost:9000/api/auth/service-token `
  -H "Content-Type: application/json" `
  -d "{\"clientId\":\"internal-client\",\"clientSecret\":\"internal-secret\"}"
```

Use `data.accessToken` as bearer token.
Token shape depends on selected mode (`jwe` or `opaque`).

## JWKS Endpoint (service-a)

Public keys:

```text
http://localhost:9000/.well-known/jwks.json
```

Opaque introspection endpoint:

```text
POST http://localhost:9000/api/auth/introspect
Header: X-Introspection-Secret: <secret>
Body: {"token":"<ACCESS_TOKEN>"}
```

## Protected API Calls

```powershell
curl -H "Authorization: Bearer <ACCESS_TOKEN>" http://localhost:9000/api/a/secure
curl -H "Authorization: Bearer <ACCESS_TOKEN>" http://localhost:9001/api/b/secure
```

## Swagger

1. Service A: `http://localhost:9000/swagger-ui.html`
2. Service B: `http://localhost:9001/swagger-ui.html`

# Two-Service OAuth2 Setup (Spring Boot)

This repo contains two services:

1. `service-a` (port `9000`): OAuth2 Authorization Server + Resource Server  
2. `service-b` (port `9001`): Resource Server

Both services trust JWTs issued by `service-a`.

## Prerequisites

1. Java 21
2. PostgreSQL (external instance)
3. Node.js 20+ (for React SPA)
4. Internet access for first dependency download

## Environment variables

Set DB credentials for `service-a`:

```powershell
$env:SERVICE_A_DB_URL="jdbc:postgresql://localhost:5432/service_a"
$env:SERVICE_A_DB_USERNAME="postgres"
$env:SERVICE_A_DB_PASSWORD="postgres"
```

Optional issuer/audience overrides:

```powershell
$env:SERVICE_A_ISSUER="http://localhost:9000"
$env:SERVICE_A_AUDIENCE_A="service-a"
$env:SERVICE_A_AUDIENCE_B="service-b"
$env:SERVICE_B_AUDIENCE="service-b"
```

Optional signing keys (recommended for stable restarts):

```powershell
$env:SERVICE_A_PRIVATE_KEY_PATH="D:\\keys\\private.pem"
$env:SERVICE_A_PUBLIC_KEY_PATH="D:\\keys\\public.pem"
```

If keys are not provided, `service-a` generates an ephemeral RSA key pair at startup.

## Run services

Start `service-a`:

```powershell
cd service-a
.\mvnw spring-boot:run
```

Start `service-b` in another terminal:

```powershell
cd service-b
.\mvnw spring-boot:run
```

Start React SPA in a third terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open: `http://127.0.0.1:8085`

## Swagger UI

1. Service A Swagger UI: `http://localhost:9000/swagger-ui.html`
2. Service B Swagger UI: `http://localhost:9001/swagger-ui.html`
3. OpenAPI docs:
1. `http://localhost:9000/v3/api-docs`
2. `http://localhost:9001/v3/api-docs`

To call protected endpoints from Swagger, click **Authorize** and paste a bearer token from `service-a`.

## React SPA PKCE demo

The `frontend/` app demonstrates:

1. OAuth2 Authorization Code + PKCE login using `pkce-client`
2. Token exchange from browser (`/oauth2/token`)
3. Calling:
1. `GET /api/a/secure`
2. `GET /api/b/secure`

Notes:

1. Default `pkce-client` redirect URI in `service-a` is already `http://127.0.0.1:8085/callback`.
2. CORS is enabled in both services for `http://127.0.0.1:8085` and `http://localhost:8085`.

## Custom user registration and login API (service-a)

Create user (password is stored as BCrypt hash in DB):

```powershell
curl -X POST http://localhost:9000/api/auth/register `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"appuser\",\"password\":\"appuser123\",\"roles\":[\"APP_USER\"]}"
```

Login and get JWT:

```powershell
curl -X POST http://localhost:9000/api/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"appuser\",\"password\":\"appuser123\"}"
```

Use returned `data.accessToken` as bearer token for both services.

## Role-scope management API (service-a)

Create/update role with scopes:

```powershell
curl -X POST http://localhost:9000/api/admin/roles `
  -H "Authorization: Bearer <ACCESS_TOKEN_WITH_service.a.read>" `
  -H "Content-Type: application/json" `
  -d "{\"roleName\":\"ONLY_B\",\"description\":\"Service B only\",\"scopes\":[\"service.b.read\"]}"
```

Assign roles to user:

```powershell
curl -X POST http://localhost:9000/api/admin/users/appuser/roles `
  -H "Authorization: Bearer <ACCESS_TOKEN_WITH_service.a.read>" `
  -H "Content-Type: application/json" `
  -d "{\"roles\":[\"ONLY_B\"]}"
```

Get effective scopes derived from user roles:

```powershell
curl -X GET http://localhost:9000/api/admin/users/appuser/scopes `
  -H "Authorization: Bearer <ACCESS_TOKEN_WITH_service.a.read>"
```

## Default seeded identities

1. Demo user: `demo` / `demo1234`
2. Default role: `APP_USER` -> `service.a.read`, `service.b.read`
3. Additional roles: `SERVICE_A_READER`, `SERVICE_B_READER`
4. PKCE client: `pkce-client`
5. Client credentials client: `internal-client` / `internal-secret`

## Token flow examples

### Client Credentials

```powershell
curl -u internal-client:internal-secret `
  -d "grant_type=client_credentials&scope=service.a.read service.b.read" `
  http://localhost:9000/oauth2/token
```

Use returned `access_token`:

```powershell
curl -H "Authorization: Bearer <ACCESS_TOKEN>" http://localhost:9000/api/a/secure
curl -H "Authorization: Bearer <ACCESS_TOKEN>" http://localhost:9001/api/b/secure
```

### Authorization Code + PKCE

1. Generate verifier/challenge (any PKCE helper tool is fine).
2. Open browser:

```text
http://localhost:9000/oauth2/authorize?response_type=code&client_id=pkce-client&scope=openid%20service.a.read%20service.b.read&redirect_uri=http://127.0.0.1:8085/callback&code_challenge=<CODE_CHALLENGE>&code_challenge_method=S256&state=abc123
```

3. Log in with `demo/demo1234`.
4. Copy `code` from redirected URL.
5. Exchange token:

```powershell
curl -d "grant_type=authorization_code&client_id=pkce-client&code=<CODE>&redirect_uri=http://127.0.0.1:8085/callback&code_verifier=<CODE_VERIFIER>" `
  http://localhost:9000/oauth2/token
```

## Test

Run tests:

```powershell
cd service-a
.\mvnw test

cd ..\service-b
.\mvnw test
```

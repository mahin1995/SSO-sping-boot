# Two-Service Token Setup (JWE or Opaque)

This repo runs with 2 Spring Boot services:

1. `service-a` (`http://localhost:9000`)
   - Issues access tokens
   - Exposes encrypted JWKS payload
   - Protects `/api/a/secure`
2. `service-b` (`http://localhost:9001`)
   - Validates `service-a` tokens
   - Protects `/api/b/secure`

No OAuth2 authorization flow is used.

## Prerequisites

1. Java 21
2. PostgreSQL

## Use Separate Env Files (Windows Friendly)

1. Copy `.env.service-a.example` to `.env.service-a`
2. Copy `.env.service-b.example` to `.env.service-b`
3. Edit each file (secrets/modes must match where required)
4. Run services with scripts:

```powershell
.\scripts\run-service-a.ps1
.\scripts\run-service-b.ps1
```

By default:

1. `run-service-a.ps1` loads `.env.service-a`
2. `run-service-b.ps1` loads `.env.service-b`
3. If missing in repo root, scripts also try `service-a/.env.service-a` or `service-b/.env.service-b`

Optional (custom file):

```powershell
.\scripts\run-service-a.ps1 -EnvFile ".env.service-a"
.\scripts\run-service-b.ps1 -EnvFile ".env.service-b"
```

Backward compatibility: if service-specific env file is missing, each script falls back to `.env`.

IDE run support (without PowerShell scripts):

1. `service-a` auto-load order:
   `.env.service-a` -> `service-a/.env.service-a` -> `../service-a/.env.service-a` -> `.env`
2. `service-b` auto-load order:
   `.env.service-b` -> `service-b/.env.service-b` -> `../service-b/.env.service-b` -> `.env`
3. On startup you will see `[env-bootstrap][service-*] Loaded ... vars from ...` in console.

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

JWKS payload encryption secret:

```powershell
$env:SERVICE_A_JWKS_ENCRYPT_SECRET="change-me-jwks-secret"
$env:SERVICE_B_JWKS_DECRYPT_SECRET="change-me-jwks-secret"
```

JWT keys:

```powershell
$env:SERVICE_A_PRIVATE_KEY_PATH="keys/private.pem"
$env:SERVICE_A_PUBLIC_KEY_PATH="keys/public.pem"
$env:SERVICE_A_KEY_ID="key-v2"
```

Dev fallback (if env path not set):

1. `service-a` will also try `keys/private.pem` + `keys/public.pem`
2. Then it will try `service-a/keys/private.pem` + `service-a/keys/public.pem`
3. Paths outside the current project root are ignored by `service-a` key loader

Generate RSA key pair (Linux/macOS):

```bash
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private.pem -out keys/public.pem
```

Generate RSA key pair (Windows PowerShell + OpenSSL):

```powershell
New-Item -ItemType Directory -Path keys -Force | Out-Null
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private.pem -out keys/public.pem
```

Generate RSA key pair (Windows CMD + OpenSSL):

```cmd
if not exist keys mkdir keys
openssl genpkey -algorithm RSA -out keys\private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys\private.pem -out keys\public.pem
```

Generate RSA key pair (Windows without OpenSSL, Java 21):

```powershell
$source = @'
import java.nio.file.*;
import java.security.*;
import java.util.Base64;

public class GenRsaPem {
  public static void main(String[] args) throws Exception {
    Path dir = Paths.get("keys");
    Files.createDirectories(dir);
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair kp = kpg.generateKeyPair();
    writePem(dir.resolve("private.pem"), "PRIVATE KEY", kp.getPrivate().getEncoded());
    writePem(dir.resolve("public.pem"), "PUBLIC KEY", kp.getPublic().getEncoded());
  }
  private static void writePem(Path path, String type, byte[] der) throws Exception {
    String b64 = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(der);
    String pem = "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    Files.writeString(path, pem);
  }
}
'@
$tmp = Join-Path $env:TEMP "GenRsaPem.java"
Set-Content -Path $tmp -Value $source -Encoding Ascii
java $tmp
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

Encrypted response wrapper (same shape on both endpoints):

```text
http://localhost:9000/.well-known/jwks.json
http://localhost:9000/api/auth/jwks
```

Response example:

```json
{
  "format": "aes-gcm+base64",
  "payload": "<base64-encrypted-jwks-json>"
}
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

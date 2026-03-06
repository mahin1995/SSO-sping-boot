param(
    [string]$EnvFile = ".env"
)

& "$PSScriptRoot/load-dotenv.ps1" -Path $EnvFile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Push-Location "$PSScriptRoot/../service-a"
try {
    .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
}

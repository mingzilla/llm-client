# Read version from build.gradle
$version = Select-String -Path ".\build.gradle" -Pattern "version = '([^']+)'" | ForEach-Object { $_.Matches.Groups[1].Value }

Write-Host "Starting deployment process for version $version..." -ForegroundColor Green

# Git operations
git add .
git commit -m "Prepare for release $version"
git tag -a "v$version" -m "Release version $version"
git push origin main
git push origin "v$version"

# Gradle deployment
Write-Host "Running Gradle publish..." -ForegroundColor Yellow
.\gradlew.bat publish -x test

Write-Host "Deployment process completed!" -ForegroundColor Green
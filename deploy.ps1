$version = "1.0.0"

Write-Host "Starting deployment process for version $version..." -ForegroundColor Green

# Git operations
git add .
git commit -m "Prepare for release $version"
git tag -a "v$version" -m "Release version $version"
git push origin main
git push origin "v$version"

# Maven deployment
Write-Host "Running Maven deploy..." -ForegroundColor Yellow
mvn clean deploy -P ossrh -DskipTests

Write-Host "Deployment process completed!" -ForegroundColor Green
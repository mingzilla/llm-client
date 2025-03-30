# Read version from build.gradle
$version = Select-String -Path ".\build.gradle" -Pattern "version = '([^']+)'" | ForEach-Object { $_.Matches.Groups[1].Value }

# Verify version was found
if (-not $version) {
    Write-Host "Error: Could not find version in build.gradle" -ForegroundColor Red
    exit 1
}

Write-Host "Starting deployment process for version $version..." -ForegroundColor Green

# Check if there are uncommitted changes
$status = git status --porcelain
if ($status) {
    Write-Host "Warning: You have uncommitted changes. Please commit or stash them first." -ForegroundColor Yellow
    exit 1
}

# Check if tag already exists
$tagExists = git tag -l "v$version"
if ($tagExists) {
    Write-Host "Error: Tag v$version already exists" -ForegroundColor Red
    exit 1
}

# Git operations
try {
    git add .
    git commit -m "Prepare for release $version"
    git tag -a "v$version" -m "Release version $version"
    git push origin main
    git push origin "v$version"
} catch {
    Write-Host "Error during Git operations: $_" -ForegroundColor Red
    exit 1
}

# Gradle deployment
Write-Host "Running Gradle publish..." -ForegroundColor Yellow
try {
    .\gradlew.bat publish
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle publish failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Host "Error during Gradle publish: $_" -ForegroundColor Red
    exit 1
}

Write-Host "Deployment process completed successfully!" -ForegroundColor Green
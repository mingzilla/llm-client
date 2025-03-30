#!/bin/bash

# Read version from build.gradle
version=$(grep "version = '" build.gradle | sed "s/version = '\(.*\)'/\1/")

echo -e "\033[32mStarting deployment process for version $version...\033[0m"

# Git operations
git add .
git commit -m "Prepare for release $version"
git tag -a "v$version" -m "Release version $version"
git push origin main
git push origin "v$version"

# Gradle deployment
echo -e "\033[33mRunning Gradle publish...\033[0m"
./gradlew publish -x test

echo -e "\033[32mDeployment process completed!\033[0m"
#!/bin/bash

# Read version from build.gradle
version=$(grep "version = '" build.gradle | sed "s/version = '\(.*\)'/\1/")

# Verify version was found
if [ -z "$version" ]; then
    echo -e "\033[31mError: Could not find version in build.gradle\033[0m"
    exit 1
fi

echo -e "\033[32mStarting deployment process for version $version...\033[0m"

# Check if there are uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
    echo -e "\033[33mWarning: You have uncommitted changes. Please commit or stash them first.\033[0m"
    exit 1
fi

# Check if tag already exists
if git tag -l "v$version" | grep -q "v$version"; then
    echo -e "\033[31mError: Tag v$version already exists\033[0m"
    exit 1
fi

# Git operations
set -e  # Exit on error
git add .
git commit -m "Prepare for release $version"
git tag -a "v$version" -m "Release version $version"
git push origin main
git push origin "v$version"

# Gradle deployment
echo -e "\033[33mRunning Gradle publish...\033[0m"
if ! ./gradlew publish; then
    echo -e "\033[31mError: Gradle publish failed\033[0m"
    exit 1
fi

echo -e "\033[32mDeployment process completed successfully!\033[0m"
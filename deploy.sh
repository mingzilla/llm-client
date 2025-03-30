#!/bin/bash

version="1.0.0"

echo -e "\033[32mStarting deployment process for version $version...\033[0m"

# Git operations
git add .
git commit -m "Prepare for release $version"
git tag -a "v$version" -m "Release version $version"
git push origin main
git push origin "v$version"

# Maven deployment
echo -e "\033[33mRunning Maven deploy...\033[0m"
mvn clean deploy -P ossrh -DskipTests

echo -e "\033[32mDeployment process completed!\033[0m"
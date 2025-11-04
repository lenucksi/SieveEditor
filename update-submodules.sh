#!/bin/bash
# Update ManageSieveJ submodule to latest version

set -e

echo "Fetching latest changes from ManageSieveJ fork..."
cd lib/ManageSieveJ
git fetch origin
git checkout master
git pull origin master

echo ""
echo "ManageSieveJ submodule updated to latest commit:"
git log -1 --oneline

echo ""
echo "To commit this update to SieveEditor:"
echo "  cd ../.."
echo "  git add lib/ManageSieveJ"
echo "  git commit -m 'Update ManageSieveJ submodule to latest'"
echo ""
echo "Don't forget to rebuild:"
echo "  ./build.sh"

#!/bin/bash

set -e

echo "Beginning"
while [[ -n "${1-}" ]] ; do
  case "${1}" in
    --old-version*)
      OLD_VERSION=${1#*=}
      ;;
    --new-version*)
      NEW_VERSION=${1#*=}
      ;;
    --repository-username*)
      SONATYPE_USERNAME=${1#*=}
      ;;
    --repository-password*)
      SONATYPE_PASSWORD=${1#*=}
      ;;
    --signing-key*)
      GPG_SIGNING_KEY=${1#*=}
      ;;
    --signing-password*)
      GPG_SIGNING_PASSWORD=${1#*=}
      ;;
    *)
      echo "Unknown option '${1}'"
      exit 1
      ;;
  esac
  shift
done

if [[ ! $OLD_VERSION =~ ^[0-9]*\.[0-9]*\.[0-9]*(-[A-Z0-9]*)?$ ]]; then
    echo "You have to provide the old version (without v-prefix, e.g. 0.14.0)"
    exit 1
fi

if [[ ! $NEW_VERSION =~ ^[0-9]*\.[0-9]*\.[0-9]*(-[A-Z0-9]*)?$ ]]; then
    echo "You have to provide the new version (without v-prefix, e.g. 0.14.0)"
    exit 1
fi

if [[ $(git rev-parse --abbrev-ref HEAD) != "release-$NEW_VERSION" ]]; then
    echo "You are not on the release branch \"release-$NEW_VERSION\", aborting..."
    exit 1
fi

if [ -n "$(git status --porcelain)" ]; then
    echo "There are local, uncommitted changes, aborting..."
    exit 1
fi

echo "Old version is $OLD_VERSION. Releasing version $NEW_VERSION..."

echo Updating version in build.gradle, README.md and docs...
sed -i -e s/version\ =.*/version\ =\ \'$NEW_VERSION\'/ build.gradle
sed -i -e s/$OLD_VERSION/$NEW_VERSION/ README.md ./docs/_data/navigation.yml ./docs/_pages/getting-started.md

echo Create release news...
./gradlew createReleaseNews

if [ -n "$(git status --porcelain)" ]; then
    echo Commiting version change
    git add build.gradle README.md ./docs -- ':!docs/**.png'
    git commit -m "Update version to $VERSION"
fi

echo Tagging release "v$VERSION"
git tag "v$VERSION"
git push origin "v$VERSION"

echo Publishing ArchUnit...
./gradlew clean publishArchUnit --no-parallel -PsonatypeUsername="$SONATYPE_USERNAME" -PsonatypePassword="$SONATYPE_PASSWORD" -PsigningKey="$GPG_SIGNING_KEY" -PsigningPassword="$GPG_SIGNING_PASSWORD"

git checkout main
git merge release-"$NEW_VERSION"
git push

#!/bin/bash

set -e

echo "Beginning"
while [[ -n "${1-}" ]] ; do
  case "${1}" in
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

if [ -n "$(git status --porcelain)" ]; then
    echo "There are local, uncommitted changes, aborting..."
    exit 1
fi

./gradlew prepareRelease
VERSION="$(grep -o -P '(?<=archunit\.version=).*' gradle.properties)"

echo Committing prepared release
git add -A
git commit -s -m "prepare release $VERSION"

echo Tagging release "v$VERSION"
git tag "v$VERSION"
git push origin "v$VERSION"

echo Publishing ArchUnit...
./gradlew clean publishArchUnit --no-parallel -PactivateReleaseToMavenCentral -PsonatypeUsername="$SONATYPE_USERNAME" -PsonatypePassword="$SONATYPE_PASSWORD" -PsigningKey="$GPG_SIGNING_KEY" -PsigningPassword="$GPG_SIGNING_PASSWORD"

echo Publishing website and examples...
./gradlew publishDocs
(cd build/docs-update && git add -A && git commit -s -m "release ArchUnit $VERSION" && git push)
(cd build/example-update && git add -A && git commit -s -m "release ArchUnit $VERSION" && git push)

./gradlew setNextSnapshotVersion

echo Committing new SNAPSHOT version
git add -A
git commit -s -m "set next SNAPSHOT version"
git push origin "$(git rev-parse --abbrev-ref HEAD)"

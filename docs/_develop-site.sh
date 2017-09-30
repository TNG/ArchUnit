#! /bin/bash
#
# helper, so you don't need to remember docker-compose syntax...

# some colors to highlight certain output
GREEN=`tput setaf 2`
RED=`tput setaf 5`
RESET=`tput sgr0`

clear

echo
echo "Docker container to develop or build this website:"
echo "=================================================="
echo
echo "Please select wether to ${GREEN}develop ${RESET} or ${RED} build ${RESET} the site:"
echo
echo "${GREEN}(d)evelop ${RESET} starts a jekyll server on port 0.0.0.0:4000,"
echo "which performs incremental builds and listens for file changes."
echo
echo "${GREEN}(b)build ${RESET} build the required docker image."
echo
echo "${RED}(p)production ${RESET} produces the site with production configuration,"
echo "into ./zz-site directory."
echo "=================================================="
echo

read -p "Enter your selection (default: develop, d) : " choice


if [[ -z $choice ]]; then
    choice='develop'
fi

case "$choice" in
  b|B|build) echo "build Docker image"
                     docker-compose --file _docker-compose-dev.yml build --force-rm
                     ;;

  d|D|dev|develop) echo "develop, incremental build"
                   docker-compose --file _docker-compose-dev.yml up
                   ;;

  p|P|production)       echo "create production site"
                   docker-compose --file _docker-compose-prod.yml up
                   docker-compose --file _docker-compose-prod.yml down
                   ;;

  # catchall: abort
  *)               echo "${RED} unknown option $choice ${RESET}, aborted."
                   exit 1
                   ;;
esac

echo "Thanx."

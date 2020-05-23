FROM jekyll/jekyll:stable

MAINTAINER Peter Gafert <peter.gafert@tngtech.com>

WORKDIR /srv/jekyll

COPY Gemfile .
COPY Gemfile.lock .

RUN bundle install
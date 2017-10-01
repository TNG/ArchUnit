FROM jekyll/jekyll:3.6

MAINTAINER Peter Gafert <peter.gafert@tngtech.com>

WORKDIR /srv/jekyll

COPY Gemfile .
COPY Gemfile.lock .

RUN bundle install
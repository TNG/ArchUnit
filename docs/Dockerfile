FROM jekyll/jekyll:stable

MAINTAINER Peter Gafert <peter.gafert@archunit.org>

WORKDIR /srv/jekyll

COPY Gemfile .
COPY Gemfile.lock .

RUN bundle install
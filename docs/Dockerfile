FROM ruby:2.3-alpine

MAINTAINER Peter Gafert <peter.gafert@tngtech.com>

COPY Gemfile .
COPY Gemfile.lock .

RUN apk update && \
    apk add make gcc musl-dev nodejs python
RUN bundle install

WORKDIR /srv/jekyll
EXPOSE 4000

FROM ruby:3.3-slim

LABEL maintainer="Peter Gafert <peter.gafert@archunit.org>"

RUN apt-get update && apt-get install -y --no-install-recommends \
      build-essential \
      git \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /srv/jekyll

COPY Gemfile Gemfile.lock ./

RUN gem install bundler -v 2.3.25 \
 && bundle install

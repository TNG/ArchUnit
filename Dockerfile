FROM jekyll/jekyll:stable

MAINTAINER Peter Gafert <peter.gafert@tngtech.com>

WORKDIR /srv/jekyll

COPY Gemfile .
COPY Gemfile.lock .

RUN gem install bundler:1.17.3
RUN bundle install || true

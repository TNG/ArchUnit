---
title: Unit test your Java architecture
layout: splash
permalink: /
header:
  overlay_color: "#0563a5"
  overlay_filter: rgba(5, 99, 165, 0.9)
  overlay_image: /assets/archunit-splash.svg
  cta_label: "Start Now"
  cta_url: "/getting-started"
excerpt: "Start enforcing your architecture within 30 minutes using the test setup you already have."
---

ArchUnit is a free, simple and extensible library for checking the architecture of your Java code
using any plain Java unit test framework. 
That is, ArchUnit can check dependencies between packages and classes, layers and slices, 
check for cyclic dependencies and more. It does so by analyzing given Java bytecode, 
importing all classes into a Java code structure. You can find examples for the current release at 
[ArchUnit Examples](https://github.com/TNG/ArchUnit-Examples) and the sources on
[GitHub](https://github.com/TNG/ArchUnit).

### News

{% for i in (0..2) %}
{% assign post = site.posts[i] %}
<span class="post-date">{{ post.date | date: "%b %-d, %Y" }}</span> – <a class="post-link" href="{{ post.url }}">{{ post.title }}</a>
{% endfor %}


---
layout: splash
permalink: /
---

![ArchUnit](assets/ArchUnit-Logo.png)

ArchUnit is a free, simple and extensible library for checking the architecture of your Java code. That is, ArchUnit can check dependencies between packages and classes, layers and slices, check for cyclic dependencies and more. It does so by analyzing given Java bytecode, importing all classes into a Java code structure.
ArchUnit's main focus is to automatically test architecture and coding rules, using any plain Java unit testing framework.

### News

{% for i in (0..2) %}
{% assign post = site.posts[i] %}
<span class="post-date">{{ post.date | date: "%b %-d, %Y" }}</span> – <a class="post-link" href="{{ post.url | relative_url }}">{{ post.title }}</a>
{% endfor %}


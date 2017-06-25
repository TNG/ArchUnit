package com.tngtech.archunit.library.dependencies;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class SimpleEdge extends Edge<String, String> {
    SimpleEdge(String from, String to) {
        super(from, to);
    }

    static List<Edge<String, String>> singleEdgeList(String from, String to) {
        return Collections.<Edge<String, String>>singletonList(new SimpleEdge(from, to));
    }

    static Set<Edge<String, String>> singleEdge(String from, String to) {
        return Collections.<Edge<String, String>>singleton(new SimpleEdge(from, to));
    }
}

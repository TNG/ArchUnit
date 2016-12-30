package com.tngtech.archunit.core;

import java.io.InputStream;
import java.net.URI;

public interface ClassFileLocation {
    InputStream openStream();

    URI getUri();
}

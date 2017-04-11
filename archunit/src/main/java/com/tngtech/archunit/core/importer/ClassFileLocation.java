package com.tngtech.archunit.core.importer;

import java.io.InputStream;
import java.net.URI;

interface ClassFileLocation {
    InputStream openStream();

    URI getUri();
}

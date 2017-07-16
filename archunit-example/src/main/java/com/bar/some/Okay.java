package com.bar.some;

import com.bar.Allow;
import com.bar.evil.Evil;

@Allow
public class Okay {
    public Okay() {
        new Evil();
    }
}

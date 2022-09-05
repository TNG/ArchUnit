package com.tngtech.archunit.core.domain.packageexamples.second.sub;

import com.tngtech.archunit.core.domain.packageexamples.first.First1;
import com.tngtech.archunit.core.domain.packageexamples.second.Second2;
import com.tngtech.archunit.core.domain.packageexamples.third.sub.ThirdSub1;

@SuppressWarnings("unused")
public class SecondSub1 extends ThirdSub1 {
    Second2 second2;

    SecondSub1(First1 first1) {
    }
}

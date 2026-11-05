package com.inflectra.spiratest.addons.junitextension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compile-only stand-in for the SpiraTest JUnit 5 extension annotation.
 * See {@link SpiraTestConfiguration} for why this exists.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SpiraTestCase {
    int testCaseId();
}

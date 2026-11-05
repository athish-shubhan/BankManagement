package com.inflectra.spiratest.addons.junitextension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compile-only stand-in for the SpiraTest JUnit 5 extension annotation.
 *
 * <p>The real extension jar is not published to Maven Central, so the default
 * build compiles the tests against this stub, which does nothing. Build with
 * {@code -Pspira} (see README) to use the real jar and report results to Spira.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpiraTestConfiguration {
    String url() default "";

    String login() default "";

    String rssToken() default "";

    int projectId() default -1;

    int releaseId() default -1;

    int testSetId() default -1;
}

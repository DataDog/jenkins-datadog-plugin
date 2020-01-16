package org.datadog.jenkins.plugins.datadog.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Inspired from https://sourceforge.net/p/findbugs/feature-requests/298/#5e88
 */
@Retention(RetentionPolicy.CLASS)
public @interface SuppressFBWarnings {
    /**
     * The set of FindBugs warnings that are to be suppressed in
     * annotated element. The value can be a bug category, kind or pattern.
     *
     */
    String[] value() default {};

    /**
     * Optional documentation of the reason why the warning is suppressed
     */
    String justification() default "";
}

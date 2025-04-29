package com.quaxantis.support.ide;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the annotated element is deliberately part of the API, even if it is not used.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface API {
}

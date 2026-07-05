package io.github.connellite.jcl.pkgtest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PackageMarker {
    String value() default "";
}

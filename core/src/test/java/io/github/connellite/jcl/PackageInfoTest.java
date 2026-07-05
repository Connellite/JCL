package io.github.connellite.jcl;

import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PackageInfoTest {

    @Test
    public void packageInfoAnnotationIsVisible() throws Exception {
        JarClassLoader jcl = new JarClassLoader();
        jcl.add( "./target/test-jcl.jar" );

        Class<?> sample = jcl.loadClass( "io.github.connellite.jcl.pkgtest.Sample" );
        Class<?> marker = jcl.loadClass( "io.github.connellite.jcl.pkgtest.PackageMarker" );

        Package pkg = sample.getPackage();
        assertNotNull( pkg );

        Annotation annotation = pkg.getAnnotation( (Class) marker );
        assertNotNull( annotation );
        assertEquals( "pkgtest", annotation.getClass().getMethod( "value" ).invoke( annotation ) );
    }

    @Test
    public void packageWithoutPackageInfoHasNoAnnotation() throws Exception {
        JarClassLoader jcl = new JarClassLoader();
        jcl.add( "./target/test-jcl.jar" );

        Class<?> test = jcl.loadClass( "io.github.connellite.jcl.test.Test" );
        Class<?> marker = jcl.loadClass( "io.github.connellite.jcl.pkgtest.PackageMarker" );

        Package pkg = test.getPackage();
        assertNotNull( pkg );
        assertNull( pkg.getAnnotation( (Class) marker ) );
    }
}

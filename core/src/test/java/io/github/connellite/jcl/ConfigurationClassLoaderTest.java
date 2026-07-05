package io.github.connellite.jcl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

public class ConfigurationClassLoaderTest {
    private static final String PROBE_CLASS = "io.github.connellite.jcl.test.ClassLoaderProbe";
    private static final String TEST_JAR = "./target/test-jcl.jar";
    private static final String OSGI_BOOT_DELEGATION = "osgi.bootdelegation";
    private static final String OSGI_BOOT_DELEGATION_STRICT = "osgi.bootdelegation.strict";
    private static final String OSGI_BOOT_DELEGATION_CLASSES = "org.osgi.framework.bootdelegation";

    private final Map<String, String> originalProperties = new HashMap<>();

    @After
    public void restoreProperties() {
        for (Map.Entry<String, String> entry : originalProperties.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty( entry.getKey() );
            } else {
                System.setProperty( entry.getKey(), entry.getValue() );
            }
        }
    }

    @Test
    public void disabledLocalLoaderFallsBackToCurrentClassLoader() throws Exception {
        setProperty( JarClassLoader.LocalLoader.class.getName(), "false" );
        setProperty( AbstractClassLoader.CurrentLoader.class.getName(), "true" );
        setProperty( AbstractClassLoader.ParentLoader.class.getName(), "false" );
        setProperty( AbstractClassLoader.SystemLoader.class.getName(), "false" );
        setProperty( AbstractClassLoader.ThreadContextLoader.class.getName(), "false" );

        JarClassLoader jcl = new JarClassLoader( new String[] { TEST_JAR } );
        Class<?> loaded = jcl.loadClass( PROBE_CLASS );

        assertFalse( jcl.getLocalLoader().isEnabled() );
        assertTrue( jcl.getCurrentLoader().isEnabled() );
        assertEquals( classPathLoaderName(), loaderNameFromInstance( loaded ) );
    }

    @Test
    public void enabledThreadContextLoaderCanLoadConfiguredLibrary() throws Exception {
        setProperty( JarClassLoader.LocalLoader.class.getName(), "false" );
        setProperty( AbstractClassLoader.CurrentLoader.class.getName(), "false" );
        setProperty( AbstractClassLoader.ParentLoader.class.getName(), "false" );
        setProperty( AbstractClassLoader.SystemLoader.class.getName(), "false" );
        setProperty( AbstractClassLoader.ThreadContextLoader.class.getName(), "true" );

        ClassLoader originalContextLoader = Thread.currentThread().getContextClassLoader();
        URL testJar = new File( TEST_JAR ).toURI().toURL();
        try (URLClassLoader threadContextLoader = new URLClassLoader( new URL[] { testJar }, null )) {
            Thread.currentThread().setContextClassLoader( threadContextLoader );

            JarClassLoader jcl = new JarClassLoader();
            Class<?> loaded = jcl.loadClass( PROBE_CLASS );

            assertTrue( jcl.getThreadLoader().isEnabled() );
            assertSame( threadContextLoader, loaded.getClassLoader() );
            assertEquals( URLClassLoader.class.getName(), loaderNameFromInstance( loaded ) );
        } finally {
            Thread.currentThread().setContextClassLoader( originalContextLoader );
        }
    }

    @Test
    public void enabledOsgiBootDelegationLoadsConfiguredPackageFromParent() throws Exception {
        setProperty( OSGI_BOOT_DELEGATION, "true" );
        setProperty( OSGI_BOOT_DELEGATION_STRICT, "true" );
        setProperty( OSGI_BOOT_DELEGATION_CLASSES, "io.github.connellite.jcl.test.*" );

        JarClassLoader jcl = new JarClassLoader( new String[] { TEST_JAR } );
        Class<?> loaded = jcl.loadClass( PROBE_CLASS );

        assertTrue( jcl.getOsgiBootLoader().isEnabled() );
        assertEquals( classPathLoaderName(), loaderNameFromInstance( loaded ) );
    }

    private void setProperty(String key, String value) {
        originalProperties.putIfAbsent( key, System.getProperty( key ) );
        System.setProperty( key, value );
    }

    private static String classPathLoaderName() throws Exception {
        return ClassLoader.getSystemClassLoader().loadClass( PROBE_CLASS )
                .getClassLoader().getClass().getName();
    }

    private static String loaderNameFromInstance(Class<?> clazz) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod( "getClassLoaderName" );
        return (String) method.invoke( instance );
    }
}

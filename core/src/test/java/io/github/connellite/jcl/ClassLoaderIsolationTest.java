package io.github.connellite.jcl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

public class ClassLoaderIsolationTest {
    private static final String TEST_CLASS = "io.github.connellite.jcl.test.Test";
    private static final String TEST_JAR = "./target/test-jcl.jar";
    private static final String JCL_CLASS_LOADER = "io.github.connellite.jcl.JarClassLoader";

    @Test
    public void sameLibraryCanBeLoadedByDifferentJclClassLoaders() throws Exception {
        JarClassLoader firstLoader = new JarClassLoader( new String[] { TEST_JAR } );
        JarClassLoader secondLoader = new JarClassLoader( new String[] { TEST_JAR } );

        Class<?> firstClass = firstLoader.loadClass( TEST_CLASS );
        Class<?> secondClass = secondLoader.loadClass( TEST_CLASS );

        assertNotSame( firstClass, secondClass );
        assertNotSame( firstClass.getClassLoader(), secondClass.getClassLoader() );
        assertEquals( JCL_CLASS_LOADER, loaderNameFromInstance( firstClass ) );
        assertEquals( JCL_CLASS_LOADER, loaderNameFromInstance( secondClass ) );
    }

    @Test
    public void sameLibraryCanBeLoadedBySystemAndJclClassLoaders() throws Exception {
        Class<?> systemClass = ClassLoader.getSystemClassLoader().loadClass( TEST_CLASS );
        JarClassLoader jcl = new JarClassLoader( new String[] { TEST_JAR } );
        Class<?> jclClass = jcl.loadClass( TEST_CLASS );

        assertNotSame( systemClass, jclClass );
        assertNotSame( systemClass.getClassLoader(), jclClass.getClassLoader() );
        assertTrue( loaderNameFromInstance( systemClass ).endsWith( "AppClassLoader" ) );
        assertEquals( JCL_CLASS_LOADER, loaderNameFromInstance( jclClass ) );
    }

    private static String loaderNameFromInstance(Class<?> clazz) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod( "getClassLoaderName" );
        return (String) method.invoke( instance );
    }
}

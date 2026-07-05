package io.github.connellite.jcl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConcurrentLoadClassTest {

    @Test
    public void loadClassConcurrently() throws Exception {
        final JarClassLoader jcl = new JarClassLoader();
        jcl.add( "./target/test-jcl.jar" );

        int threads = 12;
        ExecutorService executor = Executors.newFixedThreadPool( threads );
        CountDownLatch start = new CountDownLatch( 1 );
        List<Future<Class<?>>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add( executor.submit( new Callable<Class<?>>() {
                public Class<?> call() throws Exception {
                    start.await();
                    return jcl.loadClass( "io.github.connellite.jcl.test.Test" );
                }
            } ) );
        }

        start.countDown();

        Class<?> expected = null;
        for (Future<Class<?>> future : futures) {
            Class<?> loaded = future.get( 10, TimeUnit.SECONDS );
            if (expected == null) {
                expected = loaded;
            }
            assertEquals( expected, loaded );
        }

        executor.shutdownNow();
        assertTrue( executor.awaitTermination( 10, TimeUnit.SECONDS ) );
    }

    @Test
    public void loadersCanBeAddedWhileLoading() throws Exception {
        final JarClassLoader jcl = new JarClassLoader();
        jcl.add( "./target/test-jcl.jar" );

        ExecutorService executor = Executors.newFixedThreadPool( 2 );
        CountDownLatch start = new CountDownLatch( 1 );

        Future<Class<?>> load = executor.submit( new Callable<Class<?>>() {
            public Class<?> call() throws Exception {
                start.await();
                return jcl.loadClass( "io.github.connellite.jcl.test.Test" );
            }
        } );

        Future<?> addLoader = executor.submit( new Callable<Void>() {
            public Void call() throws Exception {
                start.await();
                jcl.addLoader( new NoopLoader() );
                return null;
            }
        } );

        start.countDown();

        assertEquals( "io.github.connellite.jcl.test.Test", load.get( 10, TimeUnit.SECONDS ).getName() );
        addLoader.get( 10, TimeUnit.SECONDS );

        executor.shutdownNow();
        assertTrue( executor.awaitTermination( 10, TimeUnit.SECONDS ) );
    }

    private static final class NoopLoader extends ProxyClassLoader {
        public Class<?> loadClass(String className, boolean resolveIt) {
            return null;
        }

        public java.io.InputStream loadResource(String name) {
            return null;
        }

        public java.net.URL findResource(String name) {
            return null;
        }
    }
}

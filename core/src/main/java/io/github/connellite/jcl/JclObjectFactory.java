/**
 *
 * Copyright 2015 Kamran Zafar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.connellite.jcl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.connellite.jcl.exception.JclException;

/**
 * A factory class that loads classes from specified JarClassLoader and tries to
 * instantiate their objects
 * 
 * @author Kamran Zafar
 * 
 */
public class JclObjectFactory {
    private static boolean autoProxy = Configuration.autoProxy();
    private final transient Logger logger = LoggerFactory.getLogger(JclObjectFactory.class);

    private static final class Holder {
        private static final JclObjectFactory INSTANCE = new JclObjectFactory();
    }

    /**
     * private constructor
     */
    private JclObjectFactory() {
    }

    /**
     * Returns the instance of the singleton factory
     * 
     * @return JclObjectFactory
     */
    public static JclObjectFactory getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns the instance of the singleton factory that can be used to create
     * auto proxies for jcl-created objects
     * 
     * @return JclObjectFactory
     */
    public static JclObjectFactory getInstance(boolean autoProxy) {
        JclObjectFactory.autoProxy = autoProxy;
        return Holder.INSTANCE;
    }

    /**
     * Creates an instance of the specified class using its default constructor.
     *
     * @param jcl class loader to load the class from
     * @param className binary name of the class to instantiate
     * @return new instance
     */
    public Object create(JarClassLoader jcl, String className) {
        return create( jcl, className, (Object[]) null );
    }

    /**
     * Creates an instance of the specified class using a matching constructor.
     *
     * @param jcl class loader to load the class from
     * @param className binary name of the class to instantiate
     * @param args constructor arguments
     * @return new instance
     */
    public Object create(JarClassLoader jcl, String className, Object... args) {
        if (args == null || args.length == 0) {
            try {
                return newInstance( jcl.loadClass( className ).getDeclaredConstructor().newInstance() );
            } catch (Throwable e) {
                throw new JclException( e );
            }
        }

        Class<?>[] types = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++)
            types[i] = args[i].getClass();

        return create( jcl, className, args, types );
    }

    /**
     * Creates an instance of the specified class using a constructor matched by
     * parameter types.
     *
     * @param jcl class loader to load the class from
     * @param className binary name of the class to instantiate
     * @param args constructor arguments
     * @param types constructor parameter types
     * @return new instance
     */
    public Object create(JarClassLoader jcl, String className, Object[] args, Class<?>[] types) {
        Object obj = null;

        if (args == null || args.length == 0) {
            try {
                obj = jcl.loadClass( className ).getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                throw new JclException( e );
            }
        } else {
            try {
                obj = jcl.loadClass( className ).getConstructor( types ).newInstance( args );
            } catch (Exception e) {
                throw new JclException( e );
            }
        }

        return newInstance( obj );
    }

    /**
     * Creates an instance via a static factory method with no arguments.
     *
     * @param jcl class loader to load the class from
     * @param className binary name of the class
     * @param methodName static factory method name
     * @param args method arguments
     * @return object returned by the factory method
     */
    public Object create(JarClassLoader jcl, String className, String methodName, Object... args) {
        if (args == null || args.length == 0) {
            try {
                return newInstance( jcl.loadClass( className ).getMethod( methodName ).invoke( null ) );
            } catch (Exception e) {
                throw new JclException( e );
            }
        }
        Class<?>[] types = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++)
            types[i] = args[i].getClass();

        return create( jcl, className, methodName, args, types );
    }

    /**
     * Creates an instance via a static factory method matched by parameter types.
     *
     * @param jcl class loader to load the class from
     * @param className binary name of the class
     * @param methodName static factory method name
     * @param args method arguments
     * @param types method parameter types
     * @return object returned by the factory method
     */
    public Object create(JarClassLoader jcl, String className, String methodName, Object[] args, Class<?>[] types) {
        Object obj = null;
        if (args == null || args.length == 0) {
            try {
                obj = jcl.loadClass( className ).getMethod( methodName ).invoke( null );
            } catch (Exception e) {
                throw new JclException( e );
            }
        } else {
            try {
                obj = jcl.loadClass( className ).getMethod( methodName, types ).invoke( null, args );
            } catch (Exception e) {
                throw new JclException( e );
            }
        }

        return newInstance( obj );
    }

    /**
     * Creates a proxy
     * 
     * @param object
     * @return
     */
    private Object newInstance(Object object) {
        if (autoProxy) {

            Class<?> superClass = null;

            // Check class
            try {
                Class.forName( object.getClass().getSuperclass().getName() );
                superClass = object.getClass().getSuperclass();
            } catch (ClassNotFoundException ignored) {
            }

            Class<?>[] interfaces = object.getClass().getInterfaces();

            List<Class<?>> il = new ArrayList<>();

            // Check available interfaces
            for (Class<?> i : interfaces) {
                try {
                    Class.forName( i.getName() );
                    il.add( i );
                } catch (ClassNotFoundException ignored) {
                }
            }

            logger.debug( "Class: {}", superClass );
            logger.debug( "Class Interfaces: {}", il );

            if (superClass == null && il.isEmpty()) {
                throw new JclException( "Neither the class [" + object.getClass().getSuperclass().getName()
                        + "] nor all the implemented interfaces found in the current classloader" );
            }

            return JclUtils.createProxy( object, superClass, il.toArray( new Class[il.size()] ), null );
        }

        return object;
    }
}

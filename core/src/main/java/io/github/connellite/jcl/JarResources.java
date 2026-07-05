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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.connellite.jcl.exception.JclException;

/**
 * JarResources reads jar files and loads the class content/bytes in a HashMap
 * 
 * @author Kamran Zafar
 * 
 */
public class JarResources {

    protected Map<String, JclJarEntry> jarEntryContents;
    protected boolean collisionAllowed;

    private final transient Logger logger = LoggerFactory.getLogger( JarResources.class );

    /**
     * Default constructor
     */
    public JarResources() {
        jarEntryContents = new HashMap<>();
        collisionAllowed = Configuration.suppressCollisionException();
    }

    /**
     * @param name
     * @return URL
     */
    public URL getResourceURL(String name) {

      JclJarEntry entry = jarEntryContents.get(name);
        if (entry != null) {
          if (entry.getBaseUrl() == null) {
            throw new JclException( "non-URL accessible resource" );
        }          
            try {
                return new URL( entry.getBaseUrl() + name );
            } catch (MalformedURLException e) {
                throw new JclException( e );
            }
        }

        return null;
    }

    /**
     * @param name
     * @return byte[]
     */
    public byte[] getResource(String name) {
      JclJarEntry entry = jarEntryContents.get(name);
      if (entry != null) {
        return entry.getResourceBytes();
      }
      else {
        return null;
      }
    }

    /**
     * Returns an immutable Map of all jar resources
     * 
     * @return Map
     */
    public Map<String, byte[]> getResources() {
      
      Map<String, byte[]> resourcesAsBytes = new HashMap<>(jarEntryContents.size());
      
      for (Map.Entry<String, JclJarEntry> entry : jarEntryContents.entrySet()) {
        resourcesAsBytes.put(entry.getKey(), entry.getValue().getResourceBytes());
      }

      return resourcesAsBytes;
    }

    /**
     * Reads the specified jar file
     * 
     * @param jarFile
     */
    public void loadJar(String jarFile) {
        logger.debug( "Loading jar: {}", jarFile );

        try {
            File file = new File( jarFile );
            String baseUrl = "jar:" + file.toURI().toString() + "!/";
            try (FileInputStream fis = new FileInputStream( file )) {
                loadJar( baseUrl, fis, false );
            }
        } catch (IOException e) {
            throw new JclException( e );
        }
    }

    /**
     * Reads the jar file from a specified URL
     * 
     * @param url
     */
    public void loadJar(URL url) {
        logger.debug( "Loading jar: {}", url.toString() );

        try {
            String baseUrl = "jar:" + url + "!/";
            try (InputStream in = url.openStream()) {
                loadJar( baseUrl, in, false );
            }
        } catch (IOException e) {
            throw new JclException( e );
        }
    }

    public void loadJar(String argBaseUrl, InputStream jarStream) {
        loadJar(argBaseUrl, jarStream, true);
    }

    /**
     * Load the jar contents from InputStream
     * @param argBaseUrl 
     * 
     */
    public void loadJar(String argBaseUrl, InputStream jarStream, boolean closeStream) {
        try {
            if (closeStream) {
                try (BufferedInputStream bis = new BufferedInputStream( jarStream );
                     JarInputStream jis = new JarInputStream( bis )) {
                    readJarEntries( argBaseUrl, jis );
                }
            } else {
                readJarEntries( argBaseUrl, new JarInputStream( new BufferedInputStream( jarStream ) ) );
            }
        } catch (IOException e) {
            throw new JclException( e );
        } catch (NullPointerException e) {
            logger.debug( "Done loading." );
        }
    }

    private void readJarEntries(String argBaseUrl, JarInputStream jis) throws IOException {
        JarEntry jarEntry;
        while (( jarEntry = jis.getNextJarEntry() ) != null) {
            logger.debug( dump( jarEntry ) );

            if (jarEntry.isDirectory()) {
                continue;
            }

            if (jarEntryContents.containsKey( jarEntry.getName() )) {
                if (!collisionAllowed)
                    throw new JclException( "Class/Resource " + jarEntry.getName() + " already loaded" );
                else {
                    logger.debug( "Class/Resource {} already loaded; ignoring entry...", jarEntry.getName() );
                    continue;
                }
            }

            logger.debug( "Entry Name: {}, Entry Size: {}", jarEntry.getName(), jarEntry.getSize() );

            byte[] b = new byte[2048];
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                int len;
                while (( len = jis.read( b ) ) > 0) {
                    out.write( b, 0, len );
                }

                JclJarEntry entry = new JclJarEntry();
                entry.setBaseUrl( argBaseUrl );
                entry.setResourceBytes( out.toByteArray() );
                jarEntryContents.put( jarEntry.getName(), entry );

                logger.debug( "{}: size={}, csize={}", jarEntry.getName(), out.size(), jarEntry.getCompressedSize() );
            }
        }
    }

    /**
     * For debugging
     * 
     * @param je
     * @return String
     */
    private String dump(JarEntry je) {
        StringBuilder sb = new StringBuilder();
        if (je.isDirectory()) {
            sb.append( "d " );
        } else {
            sb.append( "f " );
        }

        if (je.getMethod() == JarEntry.STORED) {
            sb.append( "stored   " );
        } else {
            sb.append( "defalted " );
        }

        sb.append( je.getName() );
        sb.append( "\t" );
        sb.append(je.getSize());
        if (je.getMethod() == JarEntry.DEFLATED) {
            sb.append("/").append(je.getCompressedSize());
        }

        return sb.toString();
    }
}

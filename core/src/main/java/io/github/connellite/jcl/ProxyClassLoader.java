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

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kamran Zafar
 * 
 */
public abstract class ProxyClassLoader implements Comparable<ProxyClassLoader> {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final long sequence = SEQUENCE.getAndIncrement();

    // Default order
    protected int order = 5;
    // Enabled by default
    protected boolean enabled = true;

    public int getOrder() {
        return order;
    }

    /**
     * Sets delegate loading order; lower values are consulted first.
     *
     * @param order loading order for this delegate
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Loads the class from this delegate loader.
     *
     * @param className binary name of the class to load
     * @param resolveIt whether to resolve the class if found
     * @return the loaded class, or {@code null} if not found in this delegate
     */
    public abstract Class<?> loadClass(String className, boolean resolveIt);

    /**
     * Loads the resource from this delegate loader.
     *
     * @param name resource name
     * @return resource stream, or {@code null} if not found
     */
    public abstract InputStream loadResource(String name);

    /**
     * Finds the resource URL in this delegate loader.
     *
     * @param name resource name
     * @return resource URL, or {@code null} if not found
     */
    public abstract URL findResource(String name);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int compareTo(ProxyClassLoader o) {
        if (this == o) {
            return 0;
        }

        int result = Integer.compare(order, o.getOrder());
        if (result != 0) {
            return result;
        }

        return Long.compare(sequence, o.sequence);
    }
}

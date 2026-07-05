package io.github.connellite.jcl.test;

import java.io.InputStream;
import java.net.URL;

public class TestLoader extends io.github.connellite.jcl.ProxyClassLoader {

    @Override
    public Class loadClass(String className, boolean resolveIt) {
        return null;
    }

    @Override
    public InputStream loadResource(String name) {
        return null;
    }

    @Override
    public URL findResource(String name) {
        return null;
    }

}

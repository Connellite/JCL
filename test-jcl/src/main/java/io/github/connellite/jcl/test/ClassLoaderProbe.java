package io.github.connellite.jcl.test;

public class ClassLoaderProbe {
    public String getClassLoaderName() {
        return getClass().getClassLoader().getClass().getName();
    }
}

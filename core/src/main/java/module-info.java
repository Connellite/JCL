module io.github.connellite.jcl {
    requires org.slf4j;
    requires org.objenesis;
    requires net.sf.cglib;
    requires java.xml;

    exports io.github.connellite.jcl;
    exports io.github.connellite.jcl.cloner;
    exports io.github.connellite.jcl.context;
    exports io.github.connellite.jcl.exception;
    exports io.github.connellite.jcl.proxy;
    exports io.github.connellite.jcl.utils;

    opens io.github.connellite.jcl to net.sf.cglib;
    opens io.github.connellite.jcl.proxy to net.sf.cglib;
    opens io.github.connellite.jcl.cloner to org.objenesis;
}

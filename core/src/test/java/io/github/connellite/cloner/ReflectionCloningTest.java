package io.github.connellite.cloner;

import io.github.connellite.jcl.cloner.ReflectionCloning;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ReflectionCloningTest {

    static class Node {
        int value;
        String name;
    }

    static class Holder {
        Node child;
    }

    @Test
    public void clone_null_returnsNull() {
        assertNull(ReflectionCloning.clone(null));
    }

    @Test
    public void shallowClone_null_returnsNull() {
        assertNull(ReflectionCloning.shallowClone(null));
    }

    @Test
    public void clone_simplePojo_deepCopiesFields() {
        Node n = new Node();
        n.value = 42;
        n.name = "x";

        Node copy = ReflectionCloning.clone(n);

        assertNotSame(n, copy);
        assertEquals(42, copy.value);
        assertEquals("x", copy.name);
        assertSame(n.name, copy.name);
    }

    @Test
    public void shallowClone_nestedObject_sharesChildReference() {
        Node inner = new Node();
        inner.value = 7;
        Holder h = new Holder();
        h.child = inner;

        Holder copy = ReflectionCloning.shallowClone(h);

        assertNotSame(h, copy);
        assertSame(h.child, copy.child);
    }

    @Test
    public void clone_nestedObject_copiesGraph() {
        Node inner = new Node();
        inner.value = 1;
        Holder h = new Holder();
        h.child = inner;

        Holder copy = ReflectionCloning.clone(h);

        assertNotSame(h, copy);
        assertNotSame(h.child, copy.child);
        assertEquals(1, copy.child.value);
    }
}

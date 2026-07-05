package io.github.connellite.jcl.cloner;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Deep / shallow cloning via reflection.
 */
public final class ReflectionCloning {

    private static final Objenesis OBJENESIS = new ObjenesisStd();

    private static final Set<Class<?>> IGNORED_CLASSES = Set.of(
            Integer.class,
            Long.class,
            Boolean.class,
            Class.class,
            Float.class,
            Double.class,
            Character.class,
            Byte.class,
            Short.class,
            String.class,
            Void.class,
            BigDecimal.class,
            BigInteger.class,
            URI.class,
            URL.class,
            UUID.class,
            Pattern.class
    );

    private ReflectionCloning() {
    }

    public static <T> T clone(T original) {
        if (original == null) {
            return null;
        }
        Map<Object, Object> clones = new IdentityHashMap<>();
        try {
            return cloneGraph(original, clones);
        } catch (IllegalAccessException | IllegalStateException e) {
            throw new CloningException("Error during cloning of " + original, e);
        }
    }

    public static <T> T shallowClone(T original) {
        if (original == null) {
            return null;
        }
        try {
            return cloneGraph(original);
        } catch (IllegalAccessException | IllegalStateException e) {
            throw new CloningException("Error during cloning of " + original, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneGraph(T original, Map<Object, Object> clones) throws IllegalAccessException {
        if (original == null) {
            return null;
        }

        Class<T> clz = (Class<T>) original.getClass();

        if (clz.isEnum() || IGNORED_CLASSES.contains(clz)) {
            return original;
        }

        if (clones.containsKey(original)) {
            return (T) clones.get(original);
        }

        if (clz.isArray()) {
            int length = Array.getLength(original);
            T newArray = (T) Array.newInstance(clz.getComponentType(), length);
            clones.put(original, newArray);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(original, i);
                Array.set(newArray, i, cloneGraph(element, clones));
            }
            return newArray;
        }

        T newInstance = newInstance(clz);
        clones.put(original, newInstance);

        for (Field field : allFields(clz)) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            Object value = getValueField(original, field);
            setValueField(newInstance, field, cloneGraph(value, clones));
        }

        return newInstance;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneGraph(T original) throws IllegalAccessException {
        if (original == null) {
            return null;
        }

        Class<T> clz = (Class<T>) original.getClass();

        if (clz.isEnum() || IGNORED_CLASSES.contains(clz)) {
            return original;
        }

        if (clz.isArray()) {
            int length = Array.getLength(original);
            T newArray = (T) Array.newInstance(clz.getComponentType(), length);
            for (int i = 0; i < length; i++) {
                Array.set(newArray, i, Array.get(original, i));
            }
            return newArray;
        }

        T newInstance = newInstance(clz);

        for (Field field : allFields(clz)) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            Object value = getValueField(original, field);
            setValueField(newInstance, field, value);
        }

        return newInstance;
    }

    private static <T> T newInstance(Class<T> c) {
        return OBJENESIS.newInstance(c);
    }

    private static Object getValueField(Object obj, Field field) throws IllegalAccessException {
        if (!field.canAccess(obj)) {
            field.setAccessible(true);
        }
        return field.get(obj);
    }

    private static void setValueField(Object obj, Field field, Object value) throws IllegalAccessException {
        if (!field.canAccess(obj)) {
            field.setAccessible(true);
        }
        field.set(obj, value);
    }

    private static List<Field> allFields(Class<?> c) {
        List<Field> list = new LinkedList<>();
        Collections.addAll(list, c.getDeclaredFields());
        Class<?> sc = c;
        while ((sc = sc.getSuperclass()) != Object.class && sc != null) {
            Collections.addAll(list, sc.getDeclaredFields());
        }
        return list;
    }
}

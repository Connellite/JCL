package io.github.connellite.jcl.cloner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helpers around Java object serialization.
 */
public final class Serializable {

    private static final int BYTE_BUFFER_INITIAL_CAPACITY = 5120;

    private Serializable() {
    }

    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream(BYTE_BUFFER_INITIAL_CAPACITY);
             ObjectOutputStream o = new ObjectOutputStream(b)) {
            o.writeObject(obj);
            return b.toByteArray();
        }
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes);
             ObjectInputStream o = new ObjectInputStream(b)) {
            return o.readObject();
        }
    }

    public static void writeObjectToFile(Object obj, Path pathOutFile) throws IOException {
        try (OutputStream os = Files.newOutputStream(pathOutFile);
             ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(obj);
        }
    }

    public static void writeObjectToFile(Object obj, File outFile) throws IOException {
        writeObjectToFile(obj, outFile.toPath());
    }

    public static Object readObjectFromFile(Path pathInputFile) throws IOException, ClassNotFoundException {
        try (InputStream fs = Files.newInputStream(pathInputFile);
             ObjectInputStream oin = new ObjectInputStream(fs)) {
            return oin.readObject();
        }
    }

    public static Object readObjectFromFile(File inputFile) throws IOException, ClassNotFoundException {
        return readObjectFromFile(inputFile.toPath());
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(T original) {
        if (original == null) {
            return null;
        }

        try {
            byte[] bytes = serialize(original);
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (T) in.readObject();
            }
        } catch (Exception e) {
            throw new CloningException(e.getMessage(), e);
        }
    }
}

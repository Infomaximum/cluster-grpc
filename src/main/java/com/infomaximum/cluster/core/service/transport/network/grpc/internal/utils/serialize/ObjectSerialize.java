package com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.serialize;

import java.io.*;

public class ObjectSerialize {

    public static byte[] serialize(Object value) {
        ByteArrayOutputStream serializeStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(serializeStream)) {
            objectOutputStream.writeObject(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                serializeStream.close();
            } catch (IOException ignore) {
            }
        }
        return serializeStream.toByteArray();
    }

    public static Object deserialize(byte[] value) {
        InputStream sourceStream = new ByteArrayInputStream(value);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(sourceStream)) {
            return objectInputStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                sourceStream.close();
            } catch (IOException ignore) {
            }
        }
    }
}

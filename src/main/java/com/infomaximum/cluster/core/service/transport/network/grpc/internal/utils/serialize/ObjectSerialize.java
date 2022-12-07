package com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.serialize;

import java.io.*;

public class ObjectSerialize {

    public static byte[] serialize(Object value, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        try {
            byte[] result = serialize(value);
            if (result.length == 0) {
                uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), new IllegalArgumentException("Критическая ошибка! Value: " + value));
            }
            return result;
        } catch (Throwable e) {
            uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
            return null;
        }
    }

    private static byte[] serialize(Object value) throws IOException {
        try (ByteArrayOutputStream serializeStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(serializeStream)) {
                objectOutputStream.writeObject(value);
            }
            return serializeStream.toByteArray();
        }
    }

    public static Object deserialize(byte[] value, Thread.UncaughtExceptionHandler exceptionHandler) {
        try {
            return deserialize(value);
        } catch (Throwable e) {
            exceptionHandler.uncaughtException(Thread.currentThread(), e);
            return null;
        }
    }

    private static Object deserialize(byte[] value) throws IOException, ClassNotFoundException {
        try (InputStream sourceStream = new ByteArrayInputStream(value)) {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(sourceStream)) {
                return objectInputStream.readObject();
            }
        }
    }
}

package com.infomaximum.cluster.test.utils;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReaderResources {

    public static byte[] read(String file) {
        try {
            Path path = Paths.get(
                    ReaderResources.class.getClassLoader().getResource(file).toURI()
            );
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}

package com.infomaximum.cluster.test.utils;

import com.infomaximum.cluster.core.service.transport.network.grpc.exception.ClusterGrpcException;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.CertificateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CertificateUtilsTest {

    @Test
    public void allCertificatesFromPemBundleAreTrusted() {
        TrustManagerFactory trustManagerFactory = CertificateUtils.buildTrustStore(
                ReaderResources.read("ssl/ca-chain/node1-chain.crt"),
                ReaderResources.read("ssl/ca-chain/node2-bundle.crt")
        );

        X509TrustManager trustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        Set<String> subjects = Arrays.stream(trustManager.getAcceptedIssuers())
                .map(X509Certificate::getSubjectX500Principal)
                .map(Object::toString)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of("CN=node1", "CN=node2", "CN=Test Intermediate CA", "CN=Test Root CA"), subjects);
    }

    @Test
    public void emptyTrustFileIsRejected() {
        Assertions.assertThrows(ClusterGrpcException.class, () -> CertificateUtils.buildTrustStore(
                ReaderResources.read("ssl/ca-chain/node1-chain.crt"),
                new byte[0]
        ));
    }
}

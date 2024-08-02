package com.infomaximum.cluster.test.utils;

import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransport;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.BodyProcess;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.PackageBodyUtils;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageBody;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageResponse;
import com.infomaximum.cluster.utils.RandomUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageBodyUtilsTest {

    @ParameterizedTest
    @MethodSource("getArguments")
    public void cutAndGetBodyTest(byte[] bytes) {
        UUID uuid = UUID.randomUUID();
        int total = (int) Math.ceil((double) bytes.length / BodyProcess.BODY_DELIMITER);
        ArrayList<PNetPackageBody.Builder> bodyBuilders = PackageBodyUtils.cutBody(bytes, uuid, total);
        assertThat(bodyBuilders)
                .isNotNull()
                .hasSize(total)
                .allSatisfy(builder -> {
                    assertThat(builder.getTotal()).isEqualTo(total);
                    assertThat(builder.getUuidLeastSigBits()).isEqualTo(uuid.getLeastSignificantBits());
                    assertThat(builder.getUuidMostSigBits()).isEqualTo(uuid.getMostSignificantBits());
                    assertThat(builder.getPart()).isLessThanOrEqualTo(total);
                });
        for (int i = 0; i < bodyBuilders.size(); i++) {
            int part = i + 1;
            assertThat(bodyBuilders).element(i).satisfies(builder -> {
                assertThat(builder.getPart()).isEqualTo(part);
                assertThat(builder.getBody().size()).isLessThanOrEqualTo(BodyProcess.BODY_DELIMITER);
            });
        }
        int bodySize = bodyBuilders.stream()
                .mapToInt(builder -> builder.getBody().size())
                .sum();
        assertThat(bodySize).isEqualTo(bytes.length);

        PNetPackageBody[] packageBodies = bodyBuilders
                .stream()
                .map(PNetPackageBody.Builder::build)
                .toArray(PNetPackageBody[]::new);
        byte[] body = PackageBodyUtils.getBody(packageBodies);
        assertThat(bytes).isEqualTo(body);
    }

    @ParameterizedTest
    @MethodSource("getArgs")
    public void getIndexesHeavyArgsTest(byte[][] args, Set<Integer> expectedSet) {
        int argsSum = Arrays.stream(args)
                .mapToInt(bytes -> bytes.length)
                .sum();
        Set<Integer> indexesHeavyArgs = PackageBodyUtils.getIndexesHeavyArgs(args, argsSum);
        assertThat(indexesHeavyArgs)
                .isNotNull()
                .hasSize(expectedSet.size())
                .containsAll(expectedSet);
    }

    @ParameterizedTest
    @MethodSource("getArgsForRequest")
    public void getPackagesTest(byte[][] argsRequest, int expectedSize) {
        PNetPackage[] packages = PackageBodyUtils.getPackages(argsRequest, PNetPackageRequest.newBuilder());
        assertThat(packages)
                .isNotNull()
                .hasSize(expectedSize);
        Assertions.assertThat(Arrays.stream(packages)
                        .filter(PNetPackage::hasRequest)
                        .count())
                .isEqualTo(1);
        Assertions.assertThat(Arrays.stream(packages)
                        .filter(PNetPackage::hasBody)
                        .count())
                .isEqualTo(expectedSize - 1);
    }

    @ParameterizedTest
    @MethodSource("getValueForResponse")
    public void getPackagesTest(byte[] valueResponse, int expectedSize) {
        ComponentExecutorTransport.Result result = new ComponentExecutorTransport.Result(valueResponse, null);
        PNetPackage[] packages = PackageBodyUtils.getPackages(result, PNetPackageResponse.newBuilder());
        assertThat(packages)
                .isNotNull()
                .hasSize(expectedSize);
        Assertions.assertThat(Arrays.stream(packages)
                        .filter(PNetPackage::hasResponse)
                        .count())
                .isEqualTo(1);
        Assertions.assertThat(Arrays.stream(packages)
                        .filter(PNetPackage::hasBody)
                        .count())
                .isEqualTo(expectedSize - 1);
    }

    private static Stream<Arguments> getArguments() {
        byte[] body1 = new byte[BodyProcess.BODY_DELIMITER * 4];
        byte[] body2 = new byte[BodyProcess.BODY_DELIMITER - 1];
        byte[] body3 = new byte[BodyProcess.BODY_DELIMITER + 1];
        byte[] body4 = new byte[BodyProcess.BODY_DELIMITER];
        byte[] body5 = new byte[RandomUtil.random.nextInt(body1.length, body1.length * 20)];

        RandomUtil.random.nextBytes(body1);
        RandomUtil.random.nextBytes(body2);
        RandomUtil.random.nextBytes(body3);
        RandomUtil.random.nextBytes(body4);
        RandomUtil.random.nextBytes(body5);

        return Stream.of(
                Arguments.of((Object) body1),
                Arguments.of((Object) body2),
                Arguments.of((Object) body3),
                Arguments.of((Object) body4),
                Arguments.of((Object) body5)
        );
    }

    private static Stream<Arguments> getArgs() {
        byte[] params1 = new byte[1024 * 1024];
        byte[] params2 = new byte[2 * 1024 * 1024];
        byte[] params3 = new byte[BodyProcess.BODY_DELIMITER];
        byte[] params4 = new byte[3 * BodyProcess.BODY_DELIMITER];

        RandomUtil.random.nextBytes(params1);
        RandomUtil.random.nextBytes(params2);
        RandomUtil.random.nextBytes(params3);
        RandomUtil.random.nextBytes(params4);

        byte[][] args1 = new byte[][]{params1};
        byte[][] args2 = new byte[][]{params1, params2};
        byte[][] args3 = new byte[][]{params2, params3};
        byte[][] args4 = new byte[][]{params3, params4};
        byte[][] args5 = new byte[][]{params3, params4, params1, params2, params3};
        byte[][] args6 = new byte[][]{params1, params2, params1};

        Set<Integer> set1 = new HashSet<>();
        Set<Integer> set2 = new HashSet<>() {{
            add(1);
        }};
        Set<Integer> set3 = new HashSet<>() {{
            add(0);
            add(1);
            add(4);
        }};

        return Stream.of(
                Arguments.arguments(args1, set1),
                Arguments.arguments(args2, set1),
                Arguments.arguments(args3, set2),
                Arguments.arguments(args4, set2),
                Arguments.arguments(args5, set3),
                Arguments.arguments(args6, set2)
        );
    }

    private static Stream<Arguments> getArgsForRequest() {
        byte[] params1 = new byte[1024 * 1024];
        byte[] params2 = new byte[2 * 1024 * 1024];
        byte[] params3 = new byte[BodyProcess.BODY_DELIMITER];
        byte[] params4 = new byte[3 * BodyProcess.BODY_DELIMITER];
        byte[] params5 = new byte[9 * BodyProcess.BODY_DELIMITER];

        RandomUtil.random.nextBytes(params1);
        RandomUtil.random.nextBytes(params2);
        RandomUtil.random.nextBytes(params3);
        RandomUtil.random.nextBytes(params4);
        RandomUtil.random.nextBytes(params5);

        byte[][] args1 = new byte[][]{params1};
        byte[][] args2 = new byte[][]{params1, params2};
        byte[][] args3 = new byte[][]{params2, params3};
        byte[][] args4 = new byte[][]{params3, params4};
        byte[][] args5 = new byte[][]{params3, params4, params1, params2, params3};
        byte[][] args6 = new byte[][]{params1, params2, params1};
        byte[][] args7 = new byte[][]{params5};
        byte[][] args8 = new byte[][]{params4, params5};

        return Stream.of(
                Arguments.arguments(args1, 1),
                Arguments.arguments(args2, 1),
                Arguments.arguments(args3, 2),
                Arguments.arguments(args4, 4),
                Arguments.arguments(args5, 6),
                Arguments.arguments(args6, 2),
                Arguments.arguments(args7, 10),
                Arguments.arguments(args8, 13)
        );
    }

    private static Stream<Arguments> getValueForResponse() {
        byte[] body1 = new byte[BodyProcess.BODY_DELIMITER * 4];
        byte[] body2 = new byte[BodyProcess.BODY_DELIMITER - 1];
        byte[] body3 = new byte[BodyProcess.BODY_DELIMITER + 1];
        byte[] body4 = new byte[BodyProcess.BODY_DELIMITER];
        byte[] body5 = new byte[BodyProcess.BODY_DELIMITER * 20];

        RandomUtil.random.nextBytes(body1);
        RandomUtil.random.nextBytes(body2);
        RandomUtil.random.nextBytes(body3);
        RandomUtil.random.nextBytes(body4);
        RandomUtil.random.nextBytes(body5);

        return Stream.of(
                Arguments.of(body1, 4),
                Arguments.of(body2, 1),
                Arguments.of(body3, 2),
                Arguments.of(body4, 1),
                Arguments.of(body5, 20)
        );
    }
}

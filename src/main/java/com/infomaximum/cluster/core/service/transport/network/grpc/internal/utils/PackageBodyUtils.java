package com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransport;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.BodyProcess;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageBody;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageResponse;

import java.util.*;

public class PackageBodyUtils {

    public static ArrayList<PNetPackageBody.Builder> cutBody(byte[] value, UUID uuid, int total) {
        ArrayList<PNetPackageBody.Builder> result = new ArrayList<>();
        int start_position = 0;
        for (int part = 1; part <= total; part++) {
            ByteString body = ByteString.copyFrom(
                    value,
                    start_position,
                    Math.min(BodyProcess.BODY_DELIMITER, value.length - start_position)
            );
            PNetPackageBody.Builder bodyBuilder = PNetPackageBody.newBuilder()
                    .setUuidLeastSigBits(uuid.getLeastSignificantBits())
                    .setUuidMostSigBits(uuid.getMostSignificantBits())
                    .setBody(body)
                    .setPart(part)
                    .setTotal(total);
            result.add(bodyBuilder);
            start_position += BodyProcess.BODY_DELIMITER;
        }
        return result;
    }

    public static byte[] getBody(PNetPackageBody[] bodyArray) {
        int bodySize = Arrays.stream(bodyArray)
                .mapToInt(packageBody -> packageBody.getBody().size())
                .sum();
        byte[] body = new byte[bodySize];
        int pos = 0;
        for (PNetPackageBody pNetPackageBody : bodyArray) {
            if (pNetPackageBody == null) {
                throw new RuntimeException("Body is null.");
            }
            byte[] bytes = pNetPackageBody.getBody().toByteArray();
            System.arraycopy(bytes, 0, body, pos, bytes.length);
            pos += bytes.length;
        }
        return body;
    }

    public static PNetPackage[] getPackages(byte[][] args, PNetPackageRequest.Builder builder) {
        ArrayList<PNetPackageBody.Builder> bodyList = null;
        if (args != null) {
            int argsSum = 0;
            for (byte[] arg : args) {
                argsSum += arg.length;
            }
            Set<Integer> indexesHeavyArgs = (argsSum > BodyProcess.BODY_DELIMITER) ? getIndexesHeavyArgs(args, argsSum) : null;
            for (int i = 0; i < args.length; i++) {
                if (indexesHeavyArgs != null && indexesHeavyArgs.contains(i)) {
                    int total = (int) Math.ceil((double) args[i].length / BodyProcess.BODY_DELIMITER);
                    UUID uuid = UUID.randomUUID();
                    builder.addArgs(PNetPackageBody.newBuilder()
                            .setUuidLeastSigBits(uuid.getLeastSignificantBits())
                            .setUuidMostSigBits(uuid.getMostSignificantBits())
                            .setPart(0)
                            .setTotal(total));
                    if (bodyList == null) {
                        bodyList = new ArrayList<>();
                    }
                    bodyList.addAll(PackageBodyUtils.cutBody(args[i], uuid, total));
                } else {
                    builder.addArgs(PNetPackageBody.newBuilder()
                            .setBody(ByteString.copyFrom(args[i])));
                }
            }
        }

        PNetPackage[] packages = new PNetPackage[bodyList == null ? 1 : bodyList.size() + 1];
        PNetPackage requestPackage = PNetPackage.newBuilder().setRequest(builder).build();
        packages[0] = requestPackage;
        if (bodyList != null) {
            for (int i = 1; i < packages.length; i++) {
                PNetPackage bodyPackage = PNetPackage.newBuilder().setBody(bodyList.get(i - 1)).build();
                packages[i] = bodyPackage;
            }
        }
        return packages;
    }

    public static PNetPackage[] getPackages(ComponentExecutorTransport.Result result, PNetPackageResponse.Builder builder) {
        ArrayList<PNetPackageBody.Builder> bodyList = null;
        if (result.exception() != null) {
            builder.setException(ByteString.copyFrom(result.exception()));
        } else {
            if (result.value().length > BodyProcess.BODY_DELIMITER) {
                int total = (int) Math.ceil((double) result.value().length / BodyProcess.BODY_DELIMITER);
                UUID uuid = UUID.randomUUID();
                bodyList = PackageBodyUtils.cutBody(result.value(), uuid, total);
                builder.setResult(bodyList.remove(0));
            } else {
                builder.setResult(PNetPackageBody.newBuilder()
                        .setBody(ByteString.copyFrom(result.value())));
            }
        }

        PNetPackage[] packages = new PNetPackage[bodyList == null ? 1 : bodyList.size() + 1];
        PNetPackage responsePackage = PNetPackage.newBuilder().setResponse(builder).build();
        packages[0] = responsePackage;
        if (bodyList != null) {
            for (int i = 1; i < packages.length; i++) {
                PNetPackage bodyPackage = PNetPackage.newBuilder().setBody(bodyList.get(i - 1)).build();
                packages[i] = bodyPackage;
            }
        }
        return packages;
    }

    public static UUID getUuid(PNetPackageBody packageBody) {
        if (packageBody.getUuidLeastSigBits() != 0 && packageBody.getUuidMostSigBits() != 0) {
            return new UUID(packageBody.getUuidLeastSigBits(), packageBody.getUuidMostSigBits());
        }
        return null;
    }

    public static Set<Integer> getIndexesHeavyArgs(byte[][] args, int sum) {
        // Алгоритм получения списка индексов аргументов, которые необходимо поделить на пакеты
        final int[] argsSum = new int[]{sum};
        Set<Integer> indexes = new HashSet<>();
        Map<Integer, Integer> argsIndexLength = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            argsIndexLength.put(i, args[i].length);
        }
        argsIndexLength.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEach(entry -> {
                    if (argsSum[0] > BodyProcess.BODY_DELIMITER) {
                        argsSum[0] -= entry.getValue();
                        indexes.add(entry.getKey());
                    }
                });
        return indexes;
    }
}

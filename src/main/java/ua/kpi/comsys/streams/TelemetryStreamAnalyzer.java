package ua.kpi.comsys.streams;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

public class TelemetryStreamAnalyzer {

    public Optional<SensorMetric> findFirstAnomaly(Stream<SensorMetric> stream, String targetDeviceId,
            double criticalThreshold) {
        requireNotNull(stream, "stream");
        requireNotNull(targetDeviceId, "targetDeviceId");
        return stream
                .filter(metric -> targetDeviceId.equals(metric.deviceId()))
                .filter(metric -> metric.value() > criticalThreshold)
                .findFirst();
    }

    public List<SensorMetric> streamActiveMetrics(Stream<SensorMetric> stream, double normalThreshold,
            int maxCount) {
        requireNotNull(stream, "stream");
        requirePositive(maxCount, "maxCount");
        return stream
                .dropWhile(metric -> metric.value() <= normalThreshold)
                .limit(maxCount)
                .toList();
    }

    public List<Double> calculateMovingAverages(Stream<SensorMetric> stream, String metricType, int windowSize,
            int outputLimit) {
        requireNotNull(stream, "stream");
        requireNotNull(metricType, "metricType");
        requirePositive(windowSize, "windowSize");
        requirePositive(outputLimit, "outputLimit");
        return stream
                .filter(metric -> metricType.equals(metric.metricType()))
                .gather(movingAverage(windowSize))
                .limit(outputLimit)
                .toList();
    }

    private static Gatherer<SensorMetric, Deque<Double>, Double> movingAverage(int windowSize) {
        return Gatherer.ofSequential(
                ArrayDeque::new,
                (window, metric, downstream) -> {
                    window.addLast(metric.value());
                    if (window.size() > windowSize) {
                        window.removeFirst();
                    }
                    double sum = 0;
                    for (double value : window) {
                        sum += value;
                    }
                    return downstream.push(sum / window.size());
                });
    }

    private static <T> T requireNotNull(T value, String name) {
        return Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException(name + " cannot be null"));
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
    }
}

package ua.kpi.comsys.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelemetryStreamAnalyzerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final TelemetryStreamAnalyzer analyzer = new TelemetryStreamAnalyzer();

    private static SensorMetric metric(String deviceId, String metricType, double value, int second) {
        return new SensorMetric(deviceId, metricType, value, START.plusSeconds(second));
    }

    private static Stream<SensorMetric> growingValues(String deviceId, String metricType) {
        return Stream.iterate(0, i -> i + 1)
                .map(i -> metric(deviceId, metricType, i, i));
    }

    @Test
    @Timeout(2)
    void findFirstAnomalyInInfiniteStream() {
        Stream<SensorMetric> stream = Stream.iterate(0, i -> i + 1)
                .map(i -> metric(i % 2 == 0 ? "dev-A" : "dev-B", "temp", i, i));

        Optional<SensorMetric> anomaly = analyzer.findFirstAnomaly(stream, "dev-B", 100);

        assertEquals(Optional.of(101.0), anomaly.map(SensorMetric::value));
        assertEquals(Optional.of("dev-B"), anomaly.map(SensorMetric::deviceId));
    }

    @Test
    @Timeout(2)
    void findFirstAnomalyInInfiniteGeneratedStream() {
        Stream<SensorMetric> stream = Stream.concat(
                Stream.of(metric("dev-A", "temp", 20, 0), metric("dev-A", "temp", 95, 1)),
                Stream.generate(() -> metric("dev-A", "temp", 10, 2)));

        Optional<SensorMetric> anomaly = analyzer.findFirstAnomaly(stream, "dev-A", 90);

        assertEquals(Optional.of(95.0), anomaly.map(SensorMetric::value));
    }

    @Test
    void findFirstAnomalyReturnsEmptyWhenNothingFound() {
        Stream<SensorMetric> stream = Stream.of(
                metric("dev-A", "temp", 200, 0),
                metric("dev-B", "temp", 50, 1),
                metric("dev-B", "temp", 100, 2));

        Optional<SensorMetric> anomaly = analyzer.findFirstAnomaly(stream, "dev-B", 100);

        assertTrue(anomaly.isEmpty());
    }

    @Test
    void findFirstAnomalyInEmptyStreamReturnsEmpty() {
        assertTrue(analyzer.findFirstAnomaly(Stream.empty(), "dev-A", 10).isEmpty());
    }

    @Test
    void findFirstAnomalyWithNullArgumentsThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.findFirstAnomaly(null, "dev-A", 10));
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.findFirstAnomaly(Stream.empty(), null, 10));
    }

    @Test
    @Timeout(2)
    void streamActiveMetricsSkipsStableStartInInfiniteStream() {
        List<SensorMetric> result = analyzer.streamActiveMetrics(growingValues("dev-A", "temp"), 50, 3);

        assertEquals(List.of(51.0, 52.0, 53.0), result.stream().map(SensorMetric::value).toList());
    }

    @Test
    @Timeout(2)
    void streamActiveMetricsKeepsLowValuesAfterFirstActiveValue() {
        Stream<SensorMetric> stream = Stream.concat(
                Stream.of(metric("dev-A", "temp", 5, 0), metric("dev-A", "temp", 10, 1),
                        metric("dev-A", "temp", 30, 2), metric("dev-A", "temp", 1, 3)),
                Stream.generate(() -> metric("dev-A", "temp", 2, 4)));

        List<SensorMetric> result = analyzer.streamActiveMetrics(stream, 10, 4);

        assertEquals(List.of(30.0, 1.0, 2.0, 2.0), result.stream().map(SensorMetric::value).toList());
    }

    @Test
    void streamActiveMetricsReturnsLessWhenFiniteStreamEnds() {
        Stream<SensorMetric> stream = Stream.of(
                metric("dev-A", "temp", 1, 0),
                metric("dev-A", "temp", 20, 1),
                metric("dev-A", "temp", 30, 2));

        List<SensorMetric> result = analyzer.streamActiveMetrics(stream, 10, 5);

        assertEquals(List.of(20.0, 30.0), result.stream().map(SensorMetric::value).toList());
    }

    @Test
    void streamActiveMetricsReturnsEmptyWhenAllStable() {
        Stream<SensorMetric> stream = Stream.of(metric("dev-A", "temp", 1, 0), metric("dev-A", "temp", 10, 1));

        assertTrue(analyzer.streamActiveMetrics(stream, 10, 5).isEmpty());
    }

    @Test
    void streamActiveMetricsWithNullStreamThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.streamActiveMetrics(null, 10, 5));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void streamActiveMetricsWithBadMaxCountThrowsException(int maxCount) {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.streamActiveMetrics(Stream.empty(), 10, maxCount));
    }

    @Test
    @Timeout(2)
    void calculateMovingAveragesOnInfiniteStream() {
        List<Double> result = analyzer.calculateMovingAverages(growingValues("dev-A", "temp"), "temp", 3, 5);

        assertEquals(List.of(0.0, 0.5, 1.0, 2.0, 3.0), result);
    }

    @Test
    @Timeout(2)
    void calculateMovingAveragesUsesOnlyGivenMetricType() {
        Stream<SensorMetric> stream = Stream.iterate(0, i -> i + 1)
                .map(i -> i % 2 == 0
                        ? metric("dev-A", "temp", 10, i)
                        : metric("dev-A", "humidity", 1000, i));

        List<Double> result = analyzer.calculateMovingAverages(stream, "temp", 2, 3);

        assertEquals(List.of(10.0, 10.0, 10.0), result);
    }

    @Test
    @Timeout(2)
    void calculateMovingAveragesWithWindowOfOneReturnsValues() {
        Stream<SensorMetric> stream = Stream.generate(() -> metric("dev-A", "temp", 7.5, 0));

        List<Double> result = analyzer.calculateMovingAverages(stream, "temp", 1, 3);

        assertEquals(List.of(7.5, 7.5, 7.5), result);
    }

    @Test
    void calculateMovingAveragesOnFiniteStream() {
        Stream<SensorMetric> stream = Stream.of(
                metric("dev-A", "temp", 2, 0),
                metric("dev-A", "temp", 4, 1),
                metric("dev-A", "temp", 9, 2));

        List<Double> result = analyzer.calculateMovingAverages(stream, "temp", 2, 10);

        assertEquals(List.of(2.0, 3.0, 6.5), result);
    }

    @Test
    void calculateMovingAveragesWithNullArgumentsThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.calculateMovingAverages(null, "temp", 2, 2));
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.calculateMovingAverages(Stream.empty(), null, 2, 2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void calculateMovingAveragesWithBadWindowSizeThrowsException(int windowSize) {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.calculateMovingAverages(Stream.empty(), "temp", windowSize, 2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void calculateMovingAveragesWithBadOutputLimitThrowsException(int outputLimit) {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.calculateMovingAverages(Stream.empty(), "temp", 2, outputLimit));
    }
}

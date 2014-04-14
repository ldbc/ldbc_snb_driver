package com.ldbc.driver.runtime.metrics;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.concurrent.TimeUnit;

public class OperationMetricsSnapshot {
    @JsonProperty("name")
    private String name;
    @JsonProperty("unit")
    private TimeUnit durationUnit;
    @JsonProperty("count")
    private long count;
    @JsonProperty("run_time")
    private ContinuousMetricSnapshot rutTimeMetric;
    @JsonProperty("start_time_delay")
    private ContinuousMetricSnapshot startTimeDelayMetric;
    @JsonProperty("result_code")
    private DiscreteMetricSnapshot resultCodeMetric;

    private OperationMetricsSnapshot() {
    }

    public OperationMetricsSnapshot(String name,
                                    TimeUnit durationUnit,
                                    long count,
                                    ContinuousMetricSnapshot rutTimeMetric,
                                    ContinuousMetricSnapshot startTimeDelayMetric,
                                    DiscreteMetricSnapshot resultCodeMetric) {
        this.name = name;
        this.durationUnit = durationUnit;
        this.count = count;
        this.rutTimeMetric = rutTimeMetric;
        this.startTimeDelayMetric = startTimeDelayMetric;
        this.resultCodeMetric = resultCodeMetric;
    }

    public String name() {
        return name;
    }

    public TimeUnit durationUnit() {
        return durationUnit;
    }

    public long count() {
        return count;
    }

    public ContinuousMetricSnapshot runTimeMetric() {
        return rutTimeMetric;
    }

    public ContinuousMetricSnapshot startTimeDelayMetric() {
        return startTimeDelayMetric;
    }

    public DiscreteMetricSnapshot resultCodeMetric() {
        return resultCodeMetric;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OperationMetricsSnapshot that = (OperationMetricsSnapshot) o;

        if (count != that.count) return false;
        if (durationUnit != that.durationUnit) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (resultCodeMetric != null ? !resultCodeMetric.equals(that.resultCodeMetric) : that.resultCodeMetric != null)
            return false;
        if (rutTimeMetric != null ? !rutTimeMetric.equals(that.rutTimeMetric) : that.rutTimeMetric != null)
            return false;
        if (startTimeDelayMetric != null ? !startTimeDelayMetric.equals(that.startTimeDelayMetric) : that.startTimeDelayMetric != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (durationUnit != null ? durationUnit.hashCode() : 0);
        result = 31 * result + (int) (count ^ (count >>> 32));
        result = 31 * result + (rutTimeMetric != null ? rutTimeMetric.hashCode() : 0);
        result = 31 * result + (startTimeDelayMetric != null ? startTimeDelayMetric.hashCode() : 0);
        result = 31 * result + (resultCodeMetric != null ? resultCodeMetric.hashCode() : 0);
        return result;
    }
}

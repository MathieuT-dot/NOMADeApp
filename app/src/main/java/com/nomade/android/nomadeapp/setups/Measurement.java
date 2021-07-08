package com.nomade.android.nomadeapp.setups;

import java.util.Comparator;

/**
 * Measurement
 *
 * Class that describes a Measurement.
 */
public class Measurement {

    private int measurementId;
    private int measurementCategoryId;
    private int setupId;
    private int userId;
    private String name;
    private String description;
    private Integer max;
    private Integer count;
    private Long startTime;
    private Long endTime;
    private String stringStartTime;
    private String stringEndTime;

    public Measurement(int measurementId, int measurementCategoryId, int setupId, int userId, String name, String description, Integer max, Integer count, Long startTime, Long endTime, String stringStartTime, String stringEndTime) {
        this.measurementId = measurementId;
        this.measurementCategoryId = measurementCategoryId;
        this.setupId = setupId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.max = max;
        this.count = count;
        this.startTime = startTime;
        this.endTime = endTime;
        this.stringStartTime = stringStartTime;
        this.stringEndTime = stringEndTime;
    }

    public int getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId(int measurementId) {
        this.measurementId = measurementId;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getStringStartTime() {
        return stringStartTime;
    }

    public void setStringStartTime(String stringStartTime) {
        this.stringStartTime = stringStartTime;
    }

    public String getStringEndTime() {
        return stringEndTime;
    }

    public void setStringEndTime(String stringEndTime) {
        this.stringEndTime = stringEndTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSetupId() {
        return setupId;
    }

    public void setSetupId(int setupId) {
        this.setupId = setupId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public int getMeasurementCategoryId() {
        return measurementCategoryId;
    }

    public void setMeasurementCategoryId(int measurementCategoryId) {
        this.measurementCategoryId = measurementCategoryId;
    }


    public static class IdComparator implements Comparator<Measurement> {
        @Override
        public int compare(Measurement o1, Measurement o2) {
            return Integer.compare(o1.measurementId, o2.measurementId);
        }
    }

    public static class StartTimeComparator implements Comparator<Measurement> {
        @Override
        public int compare(Measurement o1, Measurement o2) {
            return Long.compare(o1.startTime, o2.startTime);
        }
    }

    public static class NameComparator implements Comparator<Measurement> {
        @Override
        public int compare(Measurement o1, Measurement o2) {
            return o1.name.compareTo(o2.name);
        }
    }

}

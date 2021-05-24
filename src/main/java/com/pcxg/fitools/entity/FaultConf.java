package com.pcxg.fitools.entity;

import lombok.Data;

@Data
public class FaultConf {
    private String faultParams;
    private FaultLocation faultLocation;
    private FaultType faultType;

    public FaultConf(String faultParams, FaultLocation faultLocation, FaultType faultType) {
        this.faultParams = faultParams;
        this.faultLocation = faultLocation;
        this.faultType = faultType;
    }

    public String getFaultParams() {
        return faultParams;
    }

    public void setFaultParams(String faultParams) {
        this.faultParams = faultParams;
    }

    public FaultLocation getFaultLocation() {
        return faultLocation;
    }

    public void setFaultLocation(FaultLocation faultLocation) {
        this.faultLocation = faultLocation;
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public void setFaultType(FaultType faultType) {
        this.faultType = faultType;
    }

    public static class FaultLocation {
        String type;
        String location;

        public FaultLocation(String type, String location) {
            this.type = type;
            this.location = location;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        @Override
        public String toString() {
            return "FaultLocation{" +
                    "type='" + type + '\'' +
                    ", location='" + location + '\'' +
                    '}';
        }
    }
    public static class FaultType {
        String level;
        String type;

        public FaultType(String level, String type) {
            this.level = level;
            this.type = type;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "FaultType{" +
                    "level='" + level + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}


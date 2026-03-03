package com.invoice.auth.dto;

import java.io.Serializable;
import java.util.List;

public class UserStatsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long totalUsers;
    private List<DataPoint> chartData;

    public UserStatsResponse() {
    }

    public UserStatsResponse(Long totalUsers, List<DataPoint> chartData) {
        this.totalUsers = totalUsers;
        this.chartData = chartData;
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public List<DataPoint> getChartData() {
        return chartData;
    }

    public void setChartData(List<DataPoint> chartData) {
        this.chartData = chartData;
    }

    public static class DataPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private String label;
        private Long count;

        public DataPoint() {
        }

        public DataPoint(String label, Long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}

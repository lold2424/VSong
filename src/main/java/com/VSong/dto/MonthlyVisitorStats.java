package com.VSong.dto;

public class MonthlyVisitorStats {

    private final Integer year;
    private final Integer month;
    private final Long totalCount;

    public MonthlyVisitorStats(Integer year, Integer month, Long totalCount) {
        this.year = year;
        this.month = month;
        this.totalCount = totalCount;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Long getTotalCount() {
        return totalCount;
    }
}

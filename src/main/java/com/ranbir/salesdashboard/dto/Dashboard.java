package com.ranbir.salesdashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record Dashboard(
        BigDecimal totalRevenue,
        long totalOrders,
        long totalUnits,
        BigDecimal avgOrderValue,
        List<LabelValue> monthly,
        List<LabelValue> byCategory,
        List<LabelValue> byRegion,
        List<LabelValue> topProducts,
        List<LabelValue> topSalespeople) {
}

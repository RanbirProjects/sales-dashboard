package com.ranbir.salesdashboard.dto;

import java.util.List;

public record Filters(List<String> regions, List<String> categories, List<String> products, List<String> salespeople) {
}

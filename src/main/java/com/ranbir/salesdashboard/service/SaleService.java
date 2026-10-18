package com.ranbir.salesdashboard.service;

import com.ranbir.salesdashboard.dto.Dashboard;
import com.ranbir.salesdashboard.dto.Filters;
import com.ranbir.salesdashboard.dto.LabelValue;
import com.ranbir.salesdashboard.model.Sale;
import com.ranbir.salesdashboard.repository.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SaleService {

    private final SaleRepository repository;

    public SaleService(SaleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Sale> search(String region, String category, LocalDate from, LocalDate to) {
        return repository.search(blankToNull(region), blankToNull(category), from, to);
    }

    public Sale create(Sale sale) {
        sale.setId(null);
        return repository.save(sale);
    }

    public Sale update(Long id, Sale incoming) {
        Sale existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale " + id + " not found"));
        existing.setOrderDate(incoming.getOrderDate());
        existing.setProduct(incoming.getProduct());
        existing.setCategory(incoming.getCategory());
        existing.setRegion(incoming.getRegion());
        existing.setSalesperson(incoming.getSalesperson());
        existing.setQuantity(incoming.getQuantity());
        existing.setUnitPrice(incoming.getUnitPrice());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale " + id + " not found");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Filters filters() {
        return new Filters(repository.findRegions(), repository.findCategories(),
                repository.findProducts(), repository.findSalespeople());
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(String region, String category, LocalDate from, LocalDate to) {
        List<Sale> sales = search(region, category, from, to);

        BigDecimal revenue = sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long units = sales.stream().mapToLong(Sale::getQuantity).sum();
        BigDecimal avg = sales.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

        return new Dashboard(
                revenue,
                sales.size(),
                units,
                avg,
                monthly(sales),
                grouped(sales, Sale::getCategory, 10),
                grouped(sales, Sale::getRegion, 10),
                grouped(sales, Sale::getProduct, 5),
                grouped(sales, Sale::getSalesperson, 5));
    }

    private List<LabelValue> monthly(List<Sale> sales) {
        Map<YearMonth, BigDecimal> map = new TreeMap<>();
        for (Sale s : sales) {
            map.merge(YearMonth.from(s.getOrderDate()), s.getTotal(), BigDecimal::add);
        }
        List<LabelValue> out = new ArrayList<>();
        map.forEach((ym, value) -> out.add(new LabelValue(ym.toString(), value)));
        return out;
    }

    private List<LabelValue> grouped(List<Sale> sales, Function<Sale, String> key, int limit) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Sale s : sales) {
            map.merge(key.apply(s), s.getTotal(), BigDecimal::add);
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new LabelValue(e.getKey(), e.getValue()))
                .toList();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

package com.ranbir.salesdashboard.controller;

import com.ranbir.salesdashboard.dto.Dashboard;
import com.ranbir.salesdashboard.dto.Filters;
import com.ranbir.salesdashboard.model.Sale;
import com.ranbir.salesdashboard.service.SaleService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping("/sales")
    public List<Sale> list(@RequestParam(required = false) String region,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.search(region, category, from, to);
    }

    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.CREATED)
    public Sale create(@Valid @RequestBody Sale sale) {
        return service.create(sale);
    }

    @PutMapping("/sales/{id}")
    public Sale update(@PathVariable Long id, @Valid @RequestBody Sale sale) {
        return service.update(id, sale);
    }

    @DeleteMapping("/sales/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam(required = false) String region,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.dashboard(region, category, from, to);
    }

    @GetMapping("/filters")
    public Filters filters() {
        return service.filters();
    }

    @GetMapping("/sales/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String region,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        StringBuilder csv = new StringBuilder("id,order_date,product,category,region,salesperson,quantity,unit_price,total\n");
        for (Sale s : service.search(region, category, from, to)) {
            csv.append(s.getId()).append(',')
               .append(s.getOrderDate()).append(',')
               .append(esc(s.getProduct())).append(',')
               .append(esc(s.getCategory())).append(',')
               .append(esc(s.getRegion())).append(',')
               .append(esc(s.getSalesperson())).append(',')
               .append(s.getQuantity()).append(',')
               .append(s.getUnitPrice()).append(',')
               .append(s.getTotal()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String esc(String v) {
        return "\"" + (v == null ? "" : v.replace("\"", "\"\"")) + "\"";
    }
}

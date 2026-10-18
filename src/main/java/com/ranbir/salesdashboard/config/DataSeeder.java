package com.ranbir.salesdashboard.config;

import com.ranbir.salesdashboard.model.Sale;
import com.ranbir.salesdashboard.repository.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Fills an empty database with realistic demo sales (last 12 months). */
@Component
public class DataSeeder implements CommandLineRunner {

    private record Product(String name, String category, int price) {}

    private static final List<Product> PRODUCTS = List.of(
            new Product("Laptop", "Electronics", 62000),
            new Product("Smartphone", "Electronics", 28000),
            new Product("Headphones", "Electronics", 4500),
            new Product("Smartwatch", "Electronics", 9500),
            new Product("Monitor", "Electronics", 14500),
            new Product("Office Chair", "Furniture", 8500),
            new Product("Standing Desk", "Furniture", 18500),
            new Product("Bookshelf", "Furniture", 6200),
            new Product("CRM License", "Software", 4200),
            new Product("Analytics Suite", "Software", 12500),
            new Product("Security Pack", "Software", 6800),
            new Product("Keyboard", "Accessories", 2800),
            new Product("Mouse", "Accessories", 1200));

    private static final List<String> REGIONS = List.of("North", "South", "East", "West", "Central");
    private static final List<String> PEOPLE = List.of("Aarav", "Priya", "Rohan", "Sneha", "Karan", "Meera");

    private final SaleRepository repository;

    public DataSeeder(SaleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();
        List<Sale> batch = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            Product p = PRODUCTS.get(rnd.nextInt(PRODUCTS.size()));
            Sale s = new Sale();
            s.setOrderDate(today.minusDays(rnd.nextInt(365)));
            s.setProduct(p.name());
            s.setCategory(p.category());
            s.setRegion(REGIONS.get(rnd.nextInt(REGIONS.size())));
            s.setSalesperson(PEOPLE.get(rnd.nextInt(PEOPLE.size())));
            s.setQuantity(1 + rnd.nextInt(5));
            double jitter = 0.9 + rnd.nextDouble() * 0.2;
            s.setUnitPrice(BigDecimal.valueOf(p.price() * jitter).setScale(2, RoundingMode.HALF_UP));
            batch.add(s);
        }
        repository.saveAll(batch);
        System.out.println("Seeded " + batch.size() + " demo sales.");
    }
}

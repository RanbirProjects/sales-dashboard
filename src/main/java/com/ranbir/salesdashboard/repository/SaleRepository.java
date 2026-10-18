package com.ranbir.salesdashboard.repository;

import com.ranbir.salesdashboard.model.Sale;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
            select s from Sale s
            where (:region is null or s.region = :region)
              and (:category is null or s.category = :category)
              and (:fromDate is null or s.orderDate >= :fromDate)
              and (:toDate is null or s.orderDate <= :toDate)
            order by s.orderDate desc, s.id desc
            """)
    List<Sale> search(@Param("region") String region,
                      @Param("category") String category,
                      @Param("fromDate") LocalDate fromDate,
                      @Param("toDate") LocalDate toDate);

    @Query("select distinct s.region from Sale s order by s.region")
    List<String> findRegions();

    @Query("select distinct s.category from Sale s order by s.category")
    List<String> findCategories();

    @Query("select distinct s.product from Sale s order by s.product")
    List<String> findProducts();

    @Query("select distinct s.salesperson from Sale s order by s.salesperson")
    List<String> findSalespeople();
}

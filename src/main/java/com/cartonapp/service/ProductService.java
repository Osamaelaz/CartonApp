package com.cartonapp.service;

import com.cartonapp.model.ProductSummary;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<ProductSummary> findAll() throws Exception;
    List<ProductSummary> findByCustomerCode(String customerCode) throws Exception;
    Optional<ProductSummary> findByCode(String code) throws Exception;
    ProductSummary create(String customerCode,String name,String boxType,java.math.BigDecimal length,java.math.BigDecimal width,java.math.BigDecimal height,int layers,int colors,java.math.BigDecimal price,String specifications) throws Exception;
    void update(ProductSummary product) throws Exception;
    void deactivate(long id) throws Exception;
    List<com.cartonapp.model.ProductSale> findSales(long productId) throws Exception;
    List<com.cartonapp.model.PricePoint> findPriceHistory(long productId) throws Exception;
}

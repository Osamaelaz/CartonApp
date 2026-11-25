package com.cartonapp.service;

import com.cartonapp.dao.ProductDao;
import com.cartonapp.model.ProductSummary;
import java.util.List;

/** Product service boundary for the Swing UI. */
public final class DatabaseProductService implements ProductService {
    private final ProductDao products;
    public DatabaseProductService(ProductDao products) { this.products = products; }
    @Override public List<ProductSummary> findAll() throws Exception { return products.findAll(); }
    @Override public List<ProductSummary> findByCustomerCode(String customerCode) throws Exception { return products.findByCustomerCode(customerCode); }
    @Override public java.util.Optional<ProductSummary> findByCode(String code) throws Exception { return products.findByCode(code); }
    @Override public ProductSummary create(String customerCode,String name,String boxType,java.math.BigDecimal length,java.math.BigDecimal width,java.math.BigDecimal height,int layers,int colors,java.math.BigDecimal price,String specifications) throws Exception {return products.create(customerCode,name,boxType,length,width,height,layers,colors,price,specifications);}
    @Override public void update(ProductSummary product) throws Exception {products.update(product);}
    @Override public void deactivate(long id) throws Exception {products.deactivate(id);}
    @Override public List<com.cartonapp.model.ProductSale> findSales(long productId) throws Exception {return products.findSales(productId);}
    @Override public List<com.cartonapp.model.PricePoint> findPriceHistory(long productId) throws Exception {return products.findPriceHistory(productId);}
}

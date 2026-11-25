package com.cartonapp.dao;

import com.cartonapp.config.DatabaseConnectionFactory;
import com.cartonapp.model.ProductSummary;
import com.cartonapp.service.ProductPricingService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.math.BigDecimal;

/** SQL access for catalogue rows only. */
public final class ProductDao {
    private final DatabaseConnectionFactory connections;

    public ProductDao(DatabaseConnectionFactory connections) { this.connections = connections; }

    public List<ProductSummary> findAll() throws SQLException { return findByCustomerCode(null); }

    /** Saved prices of a product, newest first. */
    public List<com.cartonapp.model.PricePoint> findPriceHistory(long productId) throws SQLException {
        String sql = "SELECT effective_date,unit_price FROM product_prices WHERE product_id=? ORDER BY effective_date DESC,id DESC";
        List<com.cartonapp.model.PricePoint> points = new ArrayList<>();
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, productId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    points.add(new com.cartonapp.model.PricePoint(r.getDate(1).toLocalDate(), r.getBigDecimal(2)));
            }
        }
        return points;
    }

    /** Posted invoice lines that sold this product, newest first. */
    public List<com.cartonapp.model.ProductSale> findSales(long productId) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = """
                SELECT i.invoice_number,i.invoice_date,c.name AS customer_name,l.quantity,l.unit_price,
                       COALESCE(l.discount_amount,0) AS discount_amount
                FROM invoice_lines l JOIN invoices i ON i.id=l.invoice_id JOIN customers c ON c.id=i.customer_id
                WHERE l.product_id=? AND i.status='POSTED'
                ORDER BY i.invoice_date DESC,i.id DESC,l.line_number
                """;
        List<com.cartonapp.model.ProductSale> sales = new ArrayList<>();
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, productId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    sales.add(new com.cartonapp.model.ProductSale(r.getString("invoice_number"),
                            r.getDate("invoice_date").toLocalDate(), r.getString("customer_name"),
                            r.getBigDecimal("quantity"), r.getBigDecimal("unit_price"),
                            r.getBigDecimal("discount_amount")));
            }
        }
        return sales;
    }

    public List<ProductSummary> findByCustomerCode(String customerCode) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = """
                SELECT p.id,p.code,p.name,c.code AS customer_code,c.name AS customer_name,c.industry AS customer_industry,p.box_type,
                       p.layer_count,p.print_colors,p.paper_specs,
                       p.length_cm,p.width_cm,p.height_cm,
                       COALESCE((SELECT pp.unit_price FROM product_prices pp
                         WHERE pp.product_id=p.id ORDER BY pp.effective_date DESC,pp.id DESC LIMIT 1),0) AS current_price
                FROM products p JOIN customers c ON c.id=p.customer_id
                WHERE (? IS NULL OR c.code=?) AND p.active=TRUE
                ORDER BY p.id DESC
                """;
        List<ProductSummary> results = new ArrayList<>();
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerCode);
            statement.setString(2, customerCode);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) results.add(map(result));
            }
        }
        return results;
    }

    public Optional<ProductSummary> findByCode(String code) throws SQLException {
        return findAllIncludingInactive(code).stream().findFirst();
    }

    /** Creates a product with the carton price entered by hand. */
    public ProductSummary create(String customerCode, String name, String boxType,
            BigDecimal length, BigDecimal width, BigDecimal height, int layers, int colors, BigDecimal price,
            String specifications) throws SQLException {
        SchemaUpgrades.ensure(connections);
        try (Connection c=connections.open()) { c.setAutoCommit(false); try {
            long customerId=id(c,"SELECT id FROM customers WHERE code=?",customerCode);
            long next; try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(MAX(id),0)+1 FROM products"); ResultSet r=p.executeQuery()){r.next();next=r.getLong(1);}
            String code=String.format("P-%04d", next);
            long id; String sql="INSERT INTO products(code,customer_id,name,box_type,length_cm,width_cm,height_cm,layer_count,print_colors,paper_specs) VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,code);p.setLong(2,customerId);p.setString(3,name);p.setString(4,boxType);p.setBigDecimal(5,length);p.setBigDecimal(6,width);p.setBigDecimal(7,height);p.setInt(8,layers);p.setInt(9,colors);p.setString(10,specifications==null?"":specifications);try(ResultSet r=p.executeQuery()){r.next();id=r.getLong(1);}}
            savePrice(c,id,price); c.commit(); return findByCode(code).orElseThrow();
        } catch(SQLException exception){c.rollback();throw exception;} }
    }

    public void update(ProductSummary product) throws SQLException {
        SchemaUpgrades.ensure(connections);
        try(Connection c=connections.open()){c.setAutoCommit(false);try{
            long customerId=id(c,"SELECT id FROM customers WHERE code=?",product.customerCode());
            String sql="UPDATE products SET customer_id=?,name=?,box_type=?,length_cm=?,width_cm=?,height_cm=?,layer_count=?,print_colors=?,paper_specs=?,updated_at=CURRENT_TIMESTAMP WHERE id=?";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,customerId);p.setString(2,product.name());p.setString(3,product.boxType());p.setBigDecimal(4,product.lengthCm());p.setBigDecimal(5,product.widthCm());p.setBigDecimal(6,product.heightCm());p.setInt(7,product.layerCount());p.setInt(8,product.printColors());p.setString(9,product.specifications());p.setLong(10,product.id());p.executeUpdate();}
            // The price is the one entered (or re-priced) by the user; history only grows when it changes.
            if (latestPrice(c,product.id()) == null || latestPrice(c,product.id()).compareTo(product.currentPrice()) != 0)
                savePrice(c,product.id(),product.currentPrice());
            c.commit();
        }catch(SQLException exception){c.rollback();throw exception;}}
    }

    /** Retains historical invoice links while removing the product from active lists. */
    public void deactivate(long id) throws SQLException { try(Connection c=connections.open();PreparedStatement p=c.prepareStatement("UPDATE products SET active=FALSE,updated_at=CURRENT_TIMESTAMP WHERE id=?")){p.setLong(1,id);p.executeUpdate();} }

    private List<ProductSummary> findAllIncludingInactive(String code) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql="SELECT p.id,p.code,p.name,c.code AS customer_code,c.name AS customer_name,c.industry AS customer_industry,p.box_type,p.layer_count,p.print_colors,p.paper_specs,p.length_cm,p.width_cm,p.height_cm,COALESCE((SELECT pp.unit_price FROM product_prices pp WHERE pp.product_id=p.id ORDER BY pp.effective_date DESC,pp.id DESC LIMIT 1),0) AS current_price FROM products p JOIN customers c ON c.id=p.customer_id WHERE p.code=?";
        List<ProductSummary> results=new ArrayList<>();
        try(Connection connection=connections.open();PreparedStatement statement=connection.prepareStatement(sql)){statement.setString(1,code);try(ResultSet result=statement.executeQuery()){while(result.next())results.add(map(result));}}
        return results;
    }
    private static long id(Connection c,String sql,String value) throws SQLException {try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,value);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Reference was not found");return r.getLong(1);}}}
    private static BigDecimal latestPrice(Connection c,long productId) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT unit_price FROM product_prices WHERE product_id=? ORDER BY effective_date DESC,id DESC LIMIT 1")){
            p.setLong(1,productId);
            try(ResultSet r=p.executeQuery()){return r.next()?r.getBigDecimal(1):null;}
        }
    }
    private static void savePrice(Connection c,long productId,BigDecimal price) throws SQLException {try(PreparedStatement p=c.prepareStatement("INSERT INTO product_prices(product_id,effective_date,unit_price) VALUES (?,?,?) ON CONFLICT (product_id,effective_date) DO UPDATE SET unit_price=EXCLUDED.unit_price")){p.setLong(1,productId);p.setObject(2,LocalDate.now());p.setBigDecimal(3,price);p.executeUpdate();}}

    private static ProductSummary map(ResultSet result) throws SQLException {
        return new ProductSummary(result.getLong("id"), result.getString("code"), result.getString("name"),
                result.getString("customer_code"), result.getString("customer_name"), result.getString("customer_industry"),
                result.getBigDecimal("length_cm"), result.getBigDecimal("width_cm"),
                result.getBigDecimal("height_cm"), result.getBigDecimal("current_price"),result.getString("box_type"),result.getInt("layer_count"),result.getInt("print_colors"),result.getString("paper_specs"));
    }
}

package com.cartonapp.dao;

import com.cartonapp.config.DatabaseConnectionFactory;
import com.cartonapp.model.Customer;
import com.cartonapp.model.CustomerActivityCounts;
import com.cartonapp.model.CustomerSummary;
import com.cartonapp.model.CustomerAccount;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Customer persistence only; every SQL statement uses PreparedStatement. */
public final class CustomerDao {
    private final DatabaseConnectionFactory connections;
    public CustomerDao(DatabaseConnectionFactory connections) { this.connections = connections; }

    public Optional<Customer> findByCode(String code) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = "SELECT id,code,name,industry,phone,address,opening_balance FROM customers WHERE code=?";
        try (Connection c=connections.open(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1, code);
            try (ResultSet r=p.executeQuery()) { return r.next() ? Optional.of(map(r)) : Optional.empty(); }
        }
    }

    public void update(Customer customer) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql="UPDATE customers SET name=?,industry=?,phone=?,address=?,opening_balance=?,updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection c=connections.open(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,customer.name()); p.setString(2,customer.industry()); p.setString(3,customer.phone());
            p.setString(4,customer.address()); p.setBigDecimal(5,customer.openingBalance()); p.setLong(6,customer.id());
            if (p.executeUpdate()!=1) throw new SQLException("Customer was not found: "+customer.code());
        }
    }

    /**
     * Permanently deletes a customer together with all of its activity in one transaction:
     * its invoices (and their lines), its receipts, its products and their price history.
     * Lines on other customers' invoices that used one of its products keep the item name
     * and lose only the product link.
     */
    public void delete(long id) throws SQLException {
        SchemaUpgrades.ensure(connections);
        try (Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                execute(c, """
                    UPDATE invoice_lines l
                    SET item_name=COALESCE(l.item_name,(SELECT p.name FROM products p WHERE p.id=l.product_id)),
                        product_id=NULL
                    WHERE l.product_id IN (SELECT id FROM products WHERE customer_id=?)
                      AND l.invoice_id NOT IN (SELECT id FROM invoices WHERE customer_id=?)
                    """, id, id);
                execute(c, "UPDATE receipts SET invoice_id=NULL WHERE customer_id<>? AND invoice_id IN (SELECT id FROM invoices WHERE customer_id=?)", id, id);
                execute(c, "DELETE FROM receipts WHERE customer_id=?", id);
                execute(c, "DELETE FROM invoice_lines WHERE invoice_id IN (SELECT id FROM invoices WHERE customer_id=?)", id);
                execute(c, "DELETE FROM invoices WHERE customer_id=?", id);
                execute(c, "DELETE FROM product_prices WHERE product_id IN (SELECT id FROM products WHERE customer_id=?)", id);
                execute(c, "DELETE FROM products WHERE customer_id=?", id);
                if (execute(c, "DELETE FROM customers WHERE id=?", id) != 1) throw new SQLException("العميل غير موجود");
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private static int execute(Connection c, String sql, long... values) throws SQLException {
        try (PreparedStatement p=c.prepareStatement(sql)) {
            for (int i=0;i<values.length;i++) p.setLong(i+1,values[i]);
            return p.executeUpdate();
        }
    }

    public List<Customer> findAll() throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql="SELECT id,code,name,industry,phone,address,opening_balance FROM customers ORDER BY id";
        List<Customer> results=new ArrayList<>();
        try(Connection c=connections.open(); PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery()) { while(r.next()) results.add(map(r)); }
        return results;
    }

    public List<CustomerSummary> findAllWithBalances() throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql="""
            SELECT c.id,c.code,c.name,c.industry,c.phone,c.address,c.opening_balance,
              c.opening_balance + COALESCE((SELECT SUM(""" + InvoiceDao.invoiceDue("i") + """
              ) FROM invoices i WHERE i.customer_id=c.id AND i.status='POSTED'),0)
              - COALESCE((SELECT SUM(r.amount) FROM receipts r WHERE r.customer_id=c.id AND r.status='ACTIVE'),0) AS balance
            FROM customers c ORDER BY c.id
            """;
        List<CustomerSummary> results=new ArrayList<>();
        try(Connection c=connections.open(); PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery()) { while(r.next()) results.add(new CustomerSummary(map(r),r.getBigDecimal("balance"))); }
        return results;
    }

    public CustomerActivityCounts findActivityCounts(String customerCode) throws SQLException {
        String sql="""
            SELECT
             (SELECT COUNT(*) FROM invoices i JOIN customers c ON c.id=i.customer_id WHERE c.code=?)
             + (SELECT COUNT(*) FROM receipts r JOIN customers c ON c.id=r.customer_id WHERE c.code=?) AS statement_entries,
             (SELECT COUNT(*) FROM products p JOIN customers c ON c.id=p.customer_id WHERE c.code=? AND p.active=TRUE) AS products,
             (SELECT COUNT(*) FROM invoices i JOIN customers c ON c.id=i.customer_id WHERE c.code=?) AS invoices
            """;
        try(Connection c=connections.open();PreparedStatement p=c.prepareStatement(sql)) {
            for(int i=1;i<=4;i++) p.setString(i,customerCode);
            try(ResultSet r=p.executeQuery()){r.next();return new CustomerActivityCounts(r.getInt("statement_entries"),r.getInt("products"),r.getInt("invoices"));}
        }
    }

    /** Every customer with opening debt, posted invoices and active receipts, for the customer report. */
    public List<com.cartonapp.model.CustomerAccountRow> findAllAccounts() throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = "SELECT c.id,c.code,c.name,c.industry,c.phone,c.address,c.opening_balance,"
                + " COALESCE((SELECT SUM(" + InvoiceDao.invoiceDue("i") + ") FROM invoices i"
                + " WHERE i.customer_id=c.id AND i.status='POSTED'),0) AS invoices_total,"
                + " COALESCE((SELECT SUM(r.amount) FROM receipts r WHERE r.customer_id=c.id AND r.status='ACTIVE'),0)"
                + " AS collected FROM customers c ORDER BY c.id";
        List<com.cartonapp.model.CustomerAccountRow> rows = new ArrayList<>();
        try (Connection c=connections.open(); PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery()) {
            while (r.next())
                rows.add(new com.cartonapp.model.CustomerAccountRow(map(r), new CustomerAccount(
                        r.getBigDecimal("opening_balance"), r.getBigDecimal("invoices_total"), r.getBigDecimal("collected"))));
        }
        return rows;
    }

    /** Opening debt, posted invoices (after discounts) and active receipts of one customer. */
    public CustomerAccount findAccount(String customerCode) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = "SELECT c.opening_balance,"
                + " COALESCE((SELECT SUM(" + InvoiceDao.invoiceDue("i") + ") FROM invoices i"
                + " WHERE i.customer_id=c.id AND i.status='POSTED'),0) AS invoices_total,"
                + " COALESCE((SELECT SUM(r.amount) FROM receipts r WHERE r.customer_id=c.id AND r.status='ACTIVE'),0)"
                + " AS collected FROM customers c WHERE c.code=?";
        try (Connection c=connections.open(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1, customerCode);
            try (ResultSet r=p.executeQuery()) {
                if (!r.next()) throw new SQLException("العميل غير موجود");
                return new CustomerAccount(r.getBigDecimal("opening_balance"), r.getBigDecimal("invoices_total"),
                        r.getBigDecimal("collected"));
            }
        }
    }

    public Customer create(String name,String industry,String phone,String address,BigDecimal openingBalance) throws SQLException {
        SchemaUpgrades.ensure(connections);
        BigDecimal opening = openingBalance == null ? BigDecimal.ZERO : openingBalance;
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                long nextId;
                try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(MAX(id),0)+1 FROM customers");ResultSet r=p.executeQuery()){r.next();nextId=r.getLong(1);}
                String code=String.format("C-%03d",nextId);
                String sql="INSERT INTO customers(code,name,industry,phone,address,opening_balance) VALUES (?,?,?,?,?,?) RETURNING id";
                long id;
                try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,code);p.setString(2,name);p.setString(3,industry);p.setString(4,phone);p.setString(5,address);p.setBigDecimal(6,opening);try(ResultSet r=p.executeQuery()){r.next();id=r.getLong(1);}}
                c.commit(); return new Customer(id,code,name,industry,phone,address,opening);
            } catch(SQLException exception){c.rollback();throw exception;}
        }
    }

    private static boolean hasRows(Connection c,String sql,long id) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,id);try(ResultSet r=p.executeQuery()){return r.next();}}
    }
    private static Customer map(ResultSet r) throws SQLException {
        return new Customer(r.getLong("id"),r.getString("code"),r.getString("name"),r.getString("industry"),
                r.getString("phone"),r.getString("address"),r.getBigDecimal("opening_balance"));
    }
}

package com.cartonapp.service;

import com.cartonapp.dao.CustomerDao;
import com.cartonapp.model.Customer;
import java.sql.SQLException;
import java.util.Optional;
import java.util.List;
import com.cartonapp.model.CustomerSummary;
import com.cartonapp.model.CustomerActivityCounts;

/** Database-backed implementation of customer use cases. */
public final class DatabaseCustomerService implements CustomerService {
    private final CustomerDao customers;
    public DatabaseCustomerService(CustomerDao customers) { this.customers = customers; }
    @Override public Optional<Customer> findByCode(String code) throws SQLException { return customers.findByCode(code); }
    @Override public void update(Customer customer) throws SQLException { customers.update(customer); }
    @Override public void delete(long id) throws SQLException { customers.delete(id); }
    @Override public List<Customer> findAll() throws SQLException { return customers.findAll(); }
    @Override public Customer create(String name,String industry,String phone,String address,java.math.BigDecimal openingBalance) throws SQLException { return customers.create(name,industry,phone,address,openingBalance); }
    @Override public com.cartonapp.model.CustomerAccount findAccount(String customerCode) throws SQLException { return customers.findAccount(customerCode); }
    @Override public List<com.cartonapp.model.CustomerAccountRow> findAllAccounts() throws SQLException { return customers.findAllAccounts(); }
    @Override public List<CustomerSummary> findAllWithBalances() throws SQLException { return customers.findAllWithBalances(); }
    @Override public CustomerActivityCounts findActivityCounts(String customerCode) throws SQLException { return customers.findActivityCounts(customerCode); }
}

package com.cartonapp.service;

import com.cartonapp.model.Customer;
import java.sql.SQLException;
import java.util.Optional;
import java.util.List;
import com.cartonapp.model.CustomerSummary;
import com.cartonapp.model.CustomerActivityCounts;

/** Customer use cases exposed to Swing screens. */
public interface CustomerService {
    Optional<Customer> findByCode(String code) throws SQLException;
    void update(Customer customer) throws SQLException;
    void delete(long id) throws SQLException;
    List<Customer> findAll() throws SQLException;
    Customer create(String name,String industry,String phone,String address,java.math.BigDecimal openingBalance) throws SQLException;
    com.cartonapp.model.CustomerAccount findAccount(String customerCode) throws SQLException;
    List<com.cartonapp.model.CustomerAccountRow> findAllAccounts() throws SQLException;
    List<CustomerSummary> findAllWithBalances() throws SQLException;
    CustomerActivityCounts findActivityCounts(String customerCode) throws SQLException;
}

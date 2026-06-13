package com.lms.backend.service;

import com.lms.backend.dto.CustomerDto;
import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerDto createCustomer(CustomerDto customerDto);
    CustomerDto updateCustomer(UUID id, CustomerDto customerDto);
    CustomerDto getCustomerById(UUID id);
    List<CustomerDto> getAllCustomers();
    void deleteCustomer(UUID id);
}

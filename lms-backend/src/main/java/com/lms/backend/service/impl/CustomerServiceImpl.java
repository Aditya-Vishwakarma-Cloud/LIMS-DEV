package com.lms.backend.service.impl;

import com.lms.backend.dto.CustomerDto;
import com.lms.backend.entity.Customer;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.CustomerRepository;
import com.lms.backend.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerDto createCustomer(CustomerDto customerDto) {
        Customer customer = mapToEntity(customerDto);
        Customer savedCustomer = customerRepository.save(customer);
        return mapToDto(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerDto updateCustomer(UUID id, CustomerDto customerDto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setCustomerName(customerDto.getCustomerName());
        customer.setAliasName(customerDto.getAliasName());
        customer.setAddress(customerDto.getAddress());
        customer.setCountry(customerDto.getCountry());
        customer.setState(customerDto.getState());
        customer.setCity(customerDto.getCity());
        customer.setCurrency(customerDto.getCurrency());
        customer.setArea(customerDto.getArea());
        customer.setPinCode(customerDto.getPinCode());
        customer.setCustomerType(customerDto.getCustomerType());
        customer.setBlock(customerDto.getBlock());
        customer.setBlockReason(customerDto.getBlockReason());
        customer.setSalutations(customerDto.getSalutations());
        customer.setContactPerson(customerDto.getContactPerson());
        customer.setDescription(customerDto.getDescription());
        customer.setEmailId(customerDto.getEmailId());
        customer.setMobileNumber(customerDto.getMobileNumber());
        customer.setTelephoneNumber(customerDto.getTelephoneNumber());
        customer.setVendorCode(customerDto.getVendorCode());
        customer.setTallyLedgerName(customerDto.getTallyLedgerName());
        customer.setGstNo(customerDto.getGstNo());
        customer.setDiscount(customerDto.getDiscount());
        customer.setGstNotApplicable(customerDto.isGstNotApplicable());
        customer.setSez(customerDto.isSez());
        customer.setServiceTaxNote(customerDto.getServiceTaxNote());
        customer.setPanNo(customerDto.getPanNo());
        customer.setSacNo(customerDto.getSacNo());
        customer.setSalesManager(customerDto.getSalesManager());
        customer.setDispatchMode(customerDto.getDispatchMode());
        customer.setIndustry(customerDto.getIndustry());
        customer.setAlternateEmailId(customerDto.getAlternateEmailId());
        customer.setAlternateMobileNo(customerDto.getAlternateMobileNo());
        customer.setAlternateTelephoneNo(customerDto.getAlternateTelephoneNo());
        customer.setFaxNo(customerDto.getFaxNo());
        customer.setAlternateAddress(customerDto.getAlternateAddress());

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToDto(updatedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customer.setDeleted(true);
        customerRepository.save(customer);
    }

    private Customer mapToEntity(CustomerDto dto) {
        return Customer.builder()
                .customerName(dto.getCustomerName())
                .aliasName(dto.getAliasName())
                .address(dto.getAddress())
                .country(dto.getCountry())
                .state(dto.getState())
                .city(dto.getCity())
                .currency(dto.getCurrency())
                .area(dto.getArea())
                .pinCode(dto.getPinCode())
                .customerType(dto.getCustomerType())
                .block(dto.getBlock())
                .blockReason(dto.getBlockReason())
                .salutations(dto.getSalutations())
                .contactPerson(dto.getContactPerson())
                .description(dto.getDescription())
                .emailId(dto.getEmailId())
                .mobileNumber(dto.getMobileNumber())
                .telephoneNumber(dto.getTelephoneNumber())
                .vendorCode(dto.getVendorCode())
                .tallyLedgerName(dto.getTallyLedgerName())
                .gstNo(dto.getGstNo())
                .discount(dto.getDiscount())
                .gstNotApplicable(dto.isGstNotApplicable())
                .sez(dto.isSez())
                .serviceTaxNote(dto.getServiceTaxNote())
                .panNo(dto.getPanNo())
                .sacNo(dto.getSacNo())
                .salesManager(dto.getSalesManager())
                .dispatchMode(dto.getDispatchMode())
                .industry(dto.getIndustry())
                .alternateEmailId(dto.getAlternateEmailId())
                .alternateMobileNo(dto.getAlternateMobileNo())
                .alternateTelephoneNo(dto.getAlternateTelephoneNo())
                .faxNo(dto.getFaxNo())
                .alternateAddress(dto.getAlternateAddress())
                .build();
    }

    private CustomerDto mapToDto(Customer customer) {
        return CustomerDto.builder()
                .id(customer.getId())
                .customerName(customer.getCustomerName())
                .aliasName(customer.getAliasName())
                .address(customer.getAddress())
                .country(customer.getCountry())
                .state(customer.getState())
                .city(customer.getCity())
                .currency(customer.getCurrency())
                .area(customer.getArea())
                .pinCode(customer.getPinCode())
                .customerType(customer.getCustomerType())
                .block(customer.getBlock())
                .blockReason(customer.getBlockReason())
                .salutations(customer.getSalutations())
                .contactPerson(customer.getContactPerson())
                .description(customer.getDescription())
                .emailId(customer.getEmailId())
                .mobileNumber(customer.getMobileNumber())
                .telephoneNumber(customer.getTelephoneNumber())
                .vendorCode(customer.getVendorCode())
                .tallyLedgerName(customer.getTallyLedgerName())
                .gstNo(customer.getGstNo())
                .discount(customer.getDiscount())
                .gstNotApplicable(customer.isGstNotApplicable())
                .sez(customer.isSez())
                .serviceTaxNote(customer.getServiceTaxNote())
                .panNo(customer.getPanNo())
                .sacNo(customer.getSacNo())
                .salesManager(customer.getSalesManager())
                .dispatchMode(customer.getDispatchMode())
                .industry(customer.getIndustry())
                .alternateEmailId(customer.getAlternateEmailId())
                .alternateMobileNo(customer.getAlternateMobileNo())
                .alternateTelephoneNo(customer.getAlternateTelephoneNo())
                .faxNo(customer.getFaxNo())
                .alternateAddress(customer.getAlternateAddress())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}

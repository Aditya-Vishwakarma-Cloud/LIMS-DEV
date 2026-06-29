package com.lms.backend.service.impl;

import com.lms.backend.dto.CustomerDto;
import com.lms.backend.dto.ContactPersonDto;
import com.lms.backend.entity.Customer;
import com.lms.backend.entity.ContactPerson;
import com.lms.backend.exception.ResourceNotFoundException;
import com.lms.backend.repository.CustomerRepository;
import com.lms.backend.repository.ContactPersonRepository;
import com.lms.backend.repository.ProjectRepository;
import com.lms.backend.repository.WorkOrderRepository;
import com.lms.backend.repository.SampleRepository;
import com.lms.backend.service.CustomerService;
import com.lms.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ContactPersonRepository contactPersonRepository;
    private final ProjectRepository projectRepository;
    private final WorkOrderRepository workOrderRepository;
    private final SampleRepository sampleRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CustomerDto createCustomer(CustomerDto customerDto) {
        // Validation: Unique Name
        if (customerRepository.findByCustomerName(customerDto.getCustomerName()).isPresent()) {
            throw new IllegalArgumentException("Customer name already exists: " + customerDto.getCustomerName());
        }

        // Validation: Unique GST
        if (customerDto.getGstNo() != null && !customerDto.getGstNo().trim().isEmpty() && !customerDto.isGstNotApplicable()) {
            if (customerRepository.findByGstNo(customerDto.getGstNo()).isPresent()) {
                throw new IllegalArgumentException("GST Number already registered: " + customerDto.getGstNo());
            }
        }

        String customerCode = generateCustomerCode();

        Customer customer = mapToEntity(customerDto);
        customer.setCustomerCode(customerCode);
        customer.setDeleted(false);

        // Pre-save to get UUID
        final Customer persistedCustomer = customerRepository.save(customer);

        // Map and save Contact Persons
        if (customerDto.getContactPersons() != null) {
            List<ContactPerson> contacts = customerDto.getContactPersons().stream()
                    .map(dto -> ContactPerson.builder()
                            .name(dto.getName())
                            .designation(dto.getDesignation())
                            .phone(dto.getPhone())
                            .email(dto.getEmail())
                            .customer(persistedCustomer)
                            .build())
                    .collect(Collectors.toList());
            persistedCustomer.getContactPersons().addAll(contacts);
        }

        // Check if primary contact should be set
        if (customerDto.getPrimaryContactName() != null && !customerDto.getPrimaryContactName().isEmpty()) {
            // Find contact by name in the saved list
            persistedCustomer.getContactPersons().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(customerDto.getPrimaryContactName()))
                    .findFirst()
                    .ifPresent(persistedCustomer::setPrimaryContact);
        }

        // Save again to persist relationships
        Customer savedCustomer = customerRepository.save(persistedCustomer);
        logAudit("CREATE_CUSTOMER", "Created customer: " + savedCustomer.getCustomerCode() + " (" + savedCustomer.getCustomerName() + ")");

        return mapToDto(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerDto updateCustomer(UUID id, CustomerDto customerDto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        // Validate unique name
        Optional<Customer> nameCheck = customerRepository.findByCustomerName(customerDto.getCustomerName());
        if (nameCheck.isPresent() && !nameCheck.get().getId().equals(id)) {
            throw new IllegalArgumentException("Customer name already registered by another customer: " + customerDto.getCustomerName());
        }

        // Validate unique GST
        if (customerDto.getGstNo() != null && !customerDto.getGstNo().trim().isEmpty() && !customerDto.isGstNotApplicable()) {
            Optional<Customer> gstCheck = customerRepository.findByGstNo(customerDto.getGstNo());
            if (gstCheck.isPresent() && !gstCheck.get().getId().equals(id)) {
                throw new IllegalArgumentException("GST Number already registered by another customer: " + customerDto.getGstNo());
            }
        }

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

        // Update contacts
        if (customerDto.getContactPersons() != null) {
            customer.getContactPersons().clear();
            List<ContactPerson> contacts = customerDto.getContactPersons().stream()
                    .map(dto -> {
                        ContactPerson cp = ContactPerson.builder()
                                .name(dto.getName())
                                .designation(dto.getDesignation())
                                .phone(dto.getPhone())
                                .email(dto.getEmail())
                                .customer(customer)
                                .build();
                        if (dto.getId() != null) {
                            cp.setId(dto.getId());
                        }
                        return cp;
                    })
                    .collect(Collectors.toList());
            customer.getContactPersons().addAll(contacts);
        }

        // Update primary contact reference
        if (customerDto.getPrimaryContactId() != null) {
            contactPersonRepository.findById(customerDto.getPrimaryContactId())
                    .ifPresent(customer::setPrimaryContact);
        } else if (customerDto.getPrimaryContactName() != null && !customerDto.getPrimaryContactName().isEmpty()) {
            customer.getContactPersons().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(customerDto.getPrimaryContactName()))
                    .findFirst()
                    .ifPresent(customer::setPrimaryContact);
        } else {
            customer.setPrimaryContact(null);
        }

        Customer updatedCustomer = customerRepository.save(customer);
        logAudit("UPDATE_CUSTOMER", "Updated customer: " + updatedCustomer.getCustomerCode());
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
        
        String username = getCurrentUsername();
        customer.setDeleted(true);
        customer.setDeletedAt(LocalDateTime.now());
        customer.setDeletedBy(username);
        
        customerRepository.save(customer);
        logAudit("DELETE_CUSTOMER", "Soft deleted customer: " + customer.getCustomerCode());
    }

    private synchronized String generateCustomerCode() {
        Optional<Customer> lastCustomer = customerRepository.findFirstByCustomerCodeStartingWithOrderByCustomerCodeDesc("CUS-");
        if (lastCustomer.isPresent()) {
            String code = lastCustomer.get().getCustomerCode();
            try {
                int num = Integer.parseInt(code.substring(4));
                return String.format("CUS-%04d", num + 1);
            } catch (Exception e) {
                // Fallback
            }
        }
        return "CUS-0001";
    }

    private void logAudit(String action, String details) {
        auditLogService.log(action, getCurrentUsername(), "localhost", details);
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";
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
                .contactPersons(new ArrayList<>())
                .build();
    }

    private CustomerDto mapToDto(Customer customer) {
        // Optimized count statistics
        Long projectsCount = projectRepository.countByCustomerId(customer.getId());
        Long workOrdersCount = workOrderRepository.countByCustomerId(customer.getId());
        Long samplesCount = sampleRepository.countByWorkOrderCustomerId(customer.getId());

        List<ContactPersonDto> contacts = customer.getContactPersons().stream()
                .map(c -> ContactPersonDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .designation(c.getDesignation())
                        .phone(c.getPhone())
                        .email(c.getEmail())
                        .customerId(customer.getId())
                        .build())
                .collect(Collectors.toList());

        return CustomerDto.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
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
                
                // Contact persons
                .primaryContactId(customer.getPrimaryContact() != null ? customer.getPrimaryContact().getId() : null)
                .primaryContactName(customer.getPrimaryContact() != null ? customer.getPrimaryContact().getName() : null)
                .contactPersons(contacts)

                // Stats counts
                .totalProjects(projectsCount)
                .totalWorkOrders(workOrdersCount)
                .totalSamples(samplesCount)

                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}

package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private UUID id;
    private String customerCode;

    @NotBlank(message = "Customer Name is required")
    private String customerName;

    private String aliasName;
    private String address;
    private String country;
    private String state;
    private String city;
    private String currency;
    private String area;
    private String pinCode;
    private String customerType;
    private String block;
    private String blockReason;
    private String salutations;
    private String contactPerson; // Primary contact person name (simple legacy field)
    private String description;
    private String emailId;
    private String mobileNumber;
    private String telephoneNumber;
    private String vendorCode;
    private String tallyLedgerName;
    private String gstNo;
    private String discount;
    private boolean gstNotApplicable;
    private boolean sez;
    private String serviceTaxNote;
    private String panNo;
    private String sacNo;
    private String salesManager;
    private String dispatchMode;
    private String industry;
    private String alternateEmailId;
    private String alternateMobileNo;
    private String alternateTelephoneNo;
    private String faxNo;
    private String alternateAddress;

    // Contact Persons list
    private UUID primaryContactId;
    private String primaryContactName;
    private List<ContactPersonDto> contactPersons;

    // Optimized dashboard statistics
    private Long totalProjects;
    private Long totalWorkOrders;
    private Long totalSamples;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.lms.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class Customer extends BaseEntity {

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "alias_name", length = 200)
    private String aliasName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "currency", length = 50)
    private String currency;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "pin_code", length = 20)
    private String pinCode;

    @Column(name = "customer_type", length = 50)
    private String customerType;

    @Column(name = "block", length = 100)
    private String block;

    @Column(name = "block_reason", columnDefinition = "TEXT")
    private String blockReason;

    @Column(name = "salutations", length = 50)
    private String salutations;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "email_id", length = 100)
    private String emailId;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "telephone_number", length = 20)
    private String telephoneNumber;

    @Column(name = "vendor_code", length = 50)
    private String vendorCode;

    @Column(name = "tally_ledger_name", length = 100)
    private String tallyLedgerName;

    @Column(name = "gst_no", length = 50)
    private String gstNo;

    @Column(name = "discount", length = 50)
    private String discount;

    @Column(name = "gst_not_applicable")
    @Builder.Default
    private boolean gstNotApplicable = false;

    @Column(name = "sez")
    @Builder.Default
    private boolean sez = false;

    @Column(name = "service_tax_note", columnDefinition = "TEXT")
    private String serviceTaxNote;

    @Column(name = "pan_no", length = 50)
    private String panNo;

    @Column(name = "sac_no", length = 50)
    private String sacNo;

    @Column(name = "sales_manager", length = 100)
    private String salesManager;

    @Column(name = "dispatch_mode", length = 100)
    private String dispatchMode;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "alternate_email_id", length = 100)
    private String alternateEmailId;

    @Column(name = "alternate_mobile_no", length = 20)
    private String alternateMobileNo;

    @Column(name = "alternate_telephone_no", length = 20)
    private String alternateTelephoneNo;

    @Column(name = "fax_no", length = 50)
    private String faxNo;

    @Column(name = "alternate_address", columnDefinition = "TEXT")
    private String alternateAddress;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}

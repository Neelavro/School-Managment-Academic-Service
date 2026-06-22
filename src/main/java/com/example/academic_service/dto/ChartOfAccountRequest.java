package com.example.academic_service.dto;

import com.example.academic_service.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChartOfAccountRequest {

    @NotBlank(message = "accountCode is required")
    @Size(max = 50)
    private String accountCode;

    @NotBlank(message = "accountName is required")
    @Size(max = 255)
    private String accountName;

    @NotNull(message = "accountType is required")
    private AccountType accountType;

    /** Parent account id. Null for top-level accounts. */
    private Long parentId;

    /** Defaults to false (leaf account) when null. */
    private Boolean isGroup;

    /** Defaults to true when null. */
    private Boolean isActive;

    private String description;
}

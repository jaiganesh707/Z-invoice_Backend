package com.invoice.auth.dto;

import com.invoice.auth.entity.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowUserDto {
    private Integer id;
    private String username;
    private String email;
    private String password;
    private RoleEnum role;
    private String contactNumber;
    private Integer parentUserId;
    private String uniqueKey;
}


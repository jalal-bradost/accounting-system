package com.bradox.erp.platform.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 200) String email,
        @Size(max = 200) String displayName) {}

package com.example.eshop.admin.dto;

import java.util.List;

public record StaffSessionResponse(String username, List<String> authorities) {
}

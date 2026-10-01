package com.example.eshop.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;


public record Login(
        String CriteriaValue,
        String Password
){
}

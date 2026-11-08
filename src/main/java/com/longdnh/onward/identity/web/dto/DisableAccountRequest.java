package com.longdnh.onward.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DisableAccountRequest(
        @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {

    @Override
    public String toString() {
        return "DisableAccountRequest[password=***]";
    }
}

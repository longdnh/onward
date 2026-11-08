package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import com.longdnh.donot_get_left_behind.tenancy.application.dto.OwnershipTransferResult;

public record OwnershipTransferResponse(MembershipResponse newOwner, MembershipResponse previousOwner) {

    public static OwnershipTransferResponse from(OwnershipTransferResult result) {
        return new OwnershipTransferResponse(
                MembershipResponse.from(result.newOwner()),
                MembershipResponse.from(result.previousOwner()));
    }
}

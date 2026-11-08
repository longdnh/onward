package com.longdnh.donot_get_left_behind.tenancy.application.dto;

import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;

public record OwnershipTransferResult(Membership newOwner, Membership previousOwner) {
}

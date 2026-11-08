package com.longdnh.onward.tenancy.application.dto;

import com.longdnh.onward.tenancy.domain.Membership;

public record OwnershipTransferResult(Membership newOwner, Membership previousOwner) {
}

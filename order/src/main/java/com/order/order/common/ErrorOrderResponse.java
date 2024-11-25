package com.order.order.common;

import lombok.Getter;

@Getter
public class ErrorOrderResponse implements OrderRenponse {
    private final String errorMessage;

    public ErrorOrderResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

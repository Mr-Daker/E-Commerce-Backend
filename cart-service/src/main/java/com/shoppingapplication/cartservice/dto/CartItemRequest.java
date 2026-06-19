package com.shoppingapplication.cartservice.dto;
import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
@Data public class CartItemRequest { @NotNull @Min(1) @Max(99) private Integer quantity; }

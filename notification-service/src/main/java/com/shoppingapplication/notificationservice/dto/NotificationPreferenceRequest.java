package com.shoppingapplication.notificationservice.dto;
import lombok.Data; import javax.validation.constraints.NotNull;
@Data public class NotificationPreferenceRequest { @NotNull private Boolean orderConfirmed; @NotNull private Boolean orderCancelled; @NotNull private Boolean orderFailed; }

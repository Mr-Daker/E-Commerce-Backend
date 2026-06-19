package com.shoppingapplication.notificationservice.model;
import lombok.Getter; import lombok.Setter; import javax.persistence.*;
@Entity @Table(name="t_notification_preferences") @Getter @Setter
public class NotificationPreference { @Id private String userId; @Column(nullable=false) private Boolean orderConfirmed=true; @Column(nullable=false) private Boolean orderCancelled=true; @Column(nullable=false) private Boolean orderFailed=true; }

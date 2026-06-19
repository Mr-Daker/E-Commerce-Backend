package com.shoppingapplication.notificationservice;

import com.shoppingapplication.notificationservice.repository.NotificationRepository;
import com.shoppingapplication.notificationservice.service.NotificationService;
import com.shoppingapplication.notificationservice.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceApplicationTests {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired private NotificationPreferenceRepository preferenceRepository;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
    }

    @Test
    void consumesOrderEventIdempotently() {
        String event = "{\"eventId\":\"event-1\",\"eventType\":\"ORDER_CONFIRMED\",\"orderNumber\":\"order-1\",\"userId\":\"test-user\"}";

        notificationService.consumeOrderEvent(event);
        notificationService.consumeOrderEvent(event);

        assertThat(notificationRepository.findAll()).singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getEventId()).isEqualTo("event-1");
                    assertThat(notification.getMessage()).isEqualTo("Order order-1 was confirmed.");
                });
    }

    @Test
    void rejectsMalformedEventWithoutRequeue() {
        assertThatThrownBy(() -> notificationService.consumeOrderEvent("{\"eventType\":\"ORDER_CONFIRMED\"}"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void listsNotificationsWithPaginationMetadata() throws Exception {
        notificationService.consumeOrderEvent(
                "{\"eventId\":\"event-2\",\"eventType\":\"ORDER_CANCELLED\",\"orderNumber\":\"order-2\",\"userId\":\"test-user\"}");

        mockMvc.perform(get("/api/notification?page=0&size=10").header("X-User-Id","test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].eventType").value("ORDER_CANCELLED"));
    }

    @Test
    void appliesPreferencesAndTracksReadState() throws Exception {
        mockMvc.perform(put("/api/notification/preferences").header("X-User-Id","alice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderConfirmed\":false,\"orderCancelled\":true,\"orderFailed\":true}"))
                .andExpect(status().isOk());
        notificationService.consumeOrderEvent("{\"eventId\":\"hidden\",\"eventType\":\"ORDER_CONFIRMED\",\"orderNumber\":\"o1\",\"userId\":\"alice\"}");
        notificationService.consumeOrderEvent("{\"eventId\":\"visible\",\"eventType\":\"ORDER_CANCELLED\",\"orderNumber\":\"o2\",\"userId\":\"alice\"}");
        Long id=notificationRepository.findAll().stream().filter(n->n.getVisible()).findFirst().orElseThrow().getId();
        mockMvc.perform(get("/api/notification/unread-count").header("X-User-Id","alice")).andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(patch("/api/notification/{id}/read",id).header("X-User-Id","alice")).andExpect(status().isOk()).andExpect(jsonPath("$.data.readAt").exists());
        mockMvc.perform(get("/api/notification/unread-count").header("X-User-Id","alice")).andExpect(jsonPath("$.data").value(0));
    }
}

package com.shoppingapplication.orderservice;

import com.shoppingapplication.orderservice.model.Order;
import com.shoppingapplication.orderservice.model.OrderStatus;
import com.shoppingapplication.orderservice.repository.OrderOutboxRepository;
import com.shoppingapplication.orderservice.service.OrderPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceApplicationTests {
	@Autowired
	private OrderPersistenceService persistenceService;
	@Autowired
	private OrderOutboxRepository outboxRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void savesOrderAndOutboxEventTogether() {
		Order order = new Order();
		order.setOrderNumber("outbox-test-order");
		order.setIdempotencyKey("outbox-test-key");
		order.setRequestHash("hash");
		order.setUserId("test-user");
		order.setStatus(OrderStatus.PENDING);
		order.setCreatedAt(Instant.now());
		order.setRecoveryAfter(Instant.now().plusSeconds(60));
		order.setOrderLineItemsList(List.of());

		persistenceService.saveWithEvent(order, "ORDER_PENDING");

		assertThat(outboxRepository.findAll()).singleElement()
				.satisfies(event -> {
					assertThat(event.getAggregateId()).isEqualTo("outbox-test-order");
					assertThat(event.getPayload()).contains("ORDER_PENDING");
				});
	}

}

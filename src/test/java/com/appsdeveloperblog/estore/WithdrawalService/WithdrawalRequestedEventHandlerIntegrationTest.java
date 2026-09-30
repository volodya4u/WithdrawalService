package com.appsdeveloperblog.estore.WithdrawalService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.appsdeveloperblog.estore.WithdrawalService.handler.WithdrawalRequestedEventHandler;
import com.appsdeveloperblog.ws.core.events.WithdrawalRequestedEvent;

@EmbeddedKafka(partitions = 1, topics = "withdraw-money-topic")
@SpringBootTest(properties = "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}")
class WithdrawalRequestedEventHandlerIntegrationTest {

	@Autowired
	KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	KafkaListenerEndpointRegistry listenerRegistry;

	@MockitoSpyBean
	WithdrawalRequestedEventHandler withdrawalRequestedEventHandler;

	@Test
	void handlesWithdrawalRequestedEvent() throws Exception {
		// Send only once the listener owns the partition, so the event isn't missed.
		for (MessageListenerContainer container : listenerRegistry.getListenerContainers()) {
			ContainerTestUtils.waitForAssignment(container, 1);
		}
		WithdrawalRequestedEvent event = new WithdrawalRequestedEvent("sender-1", "recipient-1", new BigDecimal("25.50"));

		kafkaTemplate.send("withdraw-money-topic", event).get();

		ArgumentCaptor<WithdrawalRequestedEvent> received = ArgumentCaptor.forClass(WithdrawalRequestedEvent.class);
		verify(withdrawalRequestedEventHandler, timeout(10000)).handle(received.capture());
		assertEquals("sender-1", received.getValue().getSenderId());
		assertEquals("recipient-1", received.getValue().getRecepientId());
		assertEquals(new BigDecimal("25.50"), received.getValue().getAmount());
	}
}

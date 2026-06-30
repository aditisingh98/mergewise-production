package com.mergewise.kafka;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.mergewise.dto.PRRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mergewise.kafka.enabled", havingValue = "true")
public class KafkaProducerService {
 private final KafkaTemplate<String,PRRequest> kafkaTemplate;
 public void send(PRRequest req){kafkaTemplate.send("pr-analysis",req);}
}
package com.mergewise.kafka;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.mergewise.dto.PRRequest;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
 private final KafkaTemplate<String,PRRequest> kafkaTemplate;
 public void send(PRRequest req){kafkaTemplate.send("pr-analysis",req);}
}
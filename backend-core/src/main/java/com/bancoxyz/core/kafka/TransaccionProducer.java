package com.bancoxyz.core.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransaccionProducer {

    private static final String TOPIC = "transacciones-bancarias";

    private final KafkaTemplate<String, TransaccionEvento> kafkaTemplate;

    public void publicarRetiro(TransaccionEvento evento) {

        kafkaTemplate.send(
                TOPIC,
                evento.cuentaId().toString(),
                evento
        );
    }
}
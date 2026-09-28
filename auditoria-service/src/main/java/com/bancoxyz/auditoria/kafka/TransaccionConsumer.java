package com.bancoxyz.auditoria.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransaccionConsumer {

    private static final String TOPIC = "transacciones-bancarias";

    @KafkaListener(
            topics = TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumirTransaccion(TransaccionEvento evento) {

        log.info("========================================");
        log.info("EVENTO KAFKA RECIBIDO");
        log.info("Evento: {}", evento.evento());
        log.info("Cuenta: {}", evento.cuentaId());
        log.info("Monto: {}", evento.monto());
        log.info("Fecha: {}", evento.fecha());
        log.info("Saldo posterior: {}", evento.saldoPosterior());
        log.info("========================================");
    }
}
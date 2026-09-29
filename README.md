# Banco XYZ - Microservicios, Service Discovery, Resiliencia, Seguridad y Arquitectura Orientada a Eventos

## Descripción

Este proyecto corresponde a la evolución de la arquitectura del sistema **Banco XYZ**, incorporando una arquitectura basada en microservicios, capacidades de **Spring Cloud**, seguridad mediante OAuth2/JWT y una arquitectura orientada a eventos utilizando **Apache Kafka**.

La solución incorpora:

- Configuración centralizada mediante **Spring Cloud Config Server**.
- Descubrimiento de servicios mediante **Netflix Eureka**.
- Balanceo de carga mediante **Spring Cloud LoadBalancer**.
- Comunicación entre servicios mediante **Spring Cloud OpenFeign**.
- Tolerancia a fallos mediante **Resilience4j**.
- Autenticación mediante **Spring Authorization Server**.
- Emisión y validación de **tokens JWT** mediante OAuth2.
- Protección de los tres BFF mediante Spring Security.
- Arquitectura orientada a eventos mediante **Apache Kafka**.
- Publicación de eventos desde **Backend Core**.
- Consumo asíncrono de eventos mediante **auditoria-service**.
- Escalabilidad mediante **Kafka Partitions y Consumer Groups**.
- Despliegue de la infraestructura Kafka mediante **Docker Compose**.
- Automatización del despliegue de Kafka mediante **GitHub Actions**.

La solución mantiene los tres canales de atención existentes:

- BFF Web.
- BFF Mobile.
- BFF ATM.

Además, se mantiene el **Backend Core** como servicio central de negocio y **Banco Batch** como componente encargado del procesamiento y carga de información hacia la base de datos.

---

# Arquitectura de la solución

La arquitectura integra los componentes de infraestructura, los canales BFF, Backend Core, autenticación, persistencia y la nueva plataforma de mensajería Kafka.

```text
                         ┌──────────────────────┐
                         │    CONFIG SERVER     │
                         │        :8888         │
                         │                      │
                         │ Configuración        │
                         │ centralizada         │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │   EUREKA DISCOVERY   │
                         │        :8761         │
                         │                      │
                         │ Service Discovery    │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
              ┌──────────┐    ┌──────────┐    ┌──────────┐
              │ BFF WEB  │    │BFF MOBILE│    │ BFF ATM  │
              │  :8082   │    │  :8083   │    │  :8084   │
              │          │    │          │    │          │
              │Resilience│    │Resilience│    │Resilience│
              │4j + JWT  │    │4j + JWT  │    │4j + JWT  │
              └────┬─────┘    └────┬─────┘    └────┬─────┘
                   │               │               │
                   └───────────────┼───────────────┘
                                   │
                          OpenFeign + LoadBalancer
                                   │
                                   ▼
                         ┌──────────────────┐
                         │  BACKEND CORE    │
                         │      :8081       │
                         │                  │
                         │ Lógica de negocio│
                         └───────┬──────────┘
                                 │
                  ┌──────────────┼──────────────┐
                  │              │              │
                  │              │              │
                  ▼              │              ▼
             ┌─────────┐         │       ┌───────────────┐
             │  MySQL  │         │       │     KAFKA     │
             │  :3307  │         │       │               │
             └─────────┘         │       │ 3 Brokers     │
                                 │       │ 3 Particiones │
                                 │       └───────┬───────┘
                                 │               │
                                 │               │
                                 │       ┌───────▼──────────────┐
                                 │       │ transacciones-       │
                                 │       │ bancarias            │
                                 │       │ Topic                │
                                 │       └──────────┬───────────┘
                                 │                  │
                                 │                  │ Eventos
                                 │                  ▼
                                 │       ┌──────────────────────┐
                                 │       │ auditoria-service    │
                                 │       │                      │
                                 │       │ Consumer Group:      │
                                 │       │ auditoria-group      │
                                 │       │                      │
                                 │       │ :8085 / :8086        │
                                 │       └──────────────────────┘
                                 │
                                 ▼
                         Procesamiento de
                           transacciones


                  ┌──────────────────────────┐
                  │       AUTH SERVER        │
                  │          :9000           │
                  │                          │
                  │ Spring Authorization     │
                  │ Server + OAuth2 + JWT    │
                  └────────────┬─────────────┘
                               │
                               │ Emite JWT
                               ▼
                    ┌───────────────────────┐
                    │ BFF WEB / MOBILE / ATM│
                    └───────────────────────┘


                  ┌──────────────────────────┐
                  │       BANCO BATCH        │
                  │        :8080             │
                  │                          │
                  │ Procesamiento batch      │
                  │ y carga de información   │
                  └────────────┬─────────────┘
                               │
                               │ Inserción /
                               │ actualización
                               ▼
                            ┌───────┐
                            │ MySQL │
                            │ :3307 │
                            └───────┘
```

---

# Flujo general

El funcionamiento de la solución se puede resumir de la siguiente manera:

1. **Banco Batch** procesa información y carga los datos correspondientes en MySQL.
2. **Backend Core** concentra la lógica de negocio y accede a la información almacenada.
3. Los **BFF Web, Mobile y ATM** consumen Backend Core mediante OpenFeign.
4. **Eureka Discovery** permite localizar dinámicamente los servicios.
5. **Resilience4j** proporciona tolerancia a fallos en las comunicaciones entre los BFF y Backend Core.
6. **Auth Server** genera tokens JWT mediante OAuth2.
7. Los BFF validan los tokens antes de permitir el acceso a sus endpoints protegidos.
8. Cuando se realiza correctamente un retiro, **Backend Core** genera un evento `RETIRO_REALIZADO`.
9. El evento es publicado en el tópico Kafka `transacciones-bancarias`.
10. **auditoria-service** consume y procesa el evento de forma asíncrona.
11. Kafka distribuye las particiones entre las instancias pertenecientes al `auditoria-group`.

---

# Arquitectura orientada a eventos

La solución utiliza una **Event-Driven Architecture** basada en Apache Kafka.

El flujo principal para las transacciones es:

```text
Cliente
   │
   ▼
BFF ATM
   │
   │ OpenFeign
   ▼
Backend Core
   │
   │ Retiro procesado
   │
   ▼
Kafka Producer
   │
   │ RETIRO_REALIZADO
   ▼
transacciones-bancarias
   │
   ├───────────────┐
   │               │
   ▼               ▼
Partición 0     Partición 1/2
   │               │
   └───────┬───────┘
           ▼
    auditoria-service
           │
           ▼
   Procesamiento asíncrono
```

El objetivo es desacoplar el procesamiento principal de la transacción de los procesos que posteriormente necesitan conocer dicha operación.

---

# Apache Kafka

Apache Kafka se utiliza como plataforma de mensajería asíncrona de la arquitectura.

La infraestructura se encuentra desplegada mediante Docker Compose en una instancia Amazon EC2.

El clúster considera:

- 3 brokers Kafka.
- 3 nodos ZooKeeper.
- Kafka UI.
- 3 particiones para `transacciones-bancarias`.
- Factor de replicación 3.
- `min.insync.replicas=2`.

La infraestructura se encuentra definida en:

```text
docker-compose.kafka.yaml
```

Kafka UI permite visualizar los brokers, tópicos, particiones, consumer groups y mensajes procesados.

---

# Tópico `transacciones-bancarias`

El tópico principal utilizado por la arquitectura de eventos es:

```text
transacciones-bancarias
```

Configuración:

```text
Particiones: 3
Factor de replicación: 3
Min ISR: 2
```

La creación del tópico se encuentra automatizada mediante el servicio `kafka-init` incluido en Docker Compose.

Esto permite que el tópico sea creado automáticamente cuando se despliega la infraestructura si todavía no existe.

---

# Evento `RETIRO_REALIZADO`

Cuando una operación de retiro es procesada correctamente por Backend Core, se genera un evento:

```text
RETIRO_REALIZADO
```

El mensaje se publica en formato JSON.

Ejemplo:

```json
{
  "evento": "RETIRO_REALIZADO",
  "cuentaId": 101,
  "monto": 5000,
  "fecha": "2026-09-27T04:30:00",
  "saldoPosterior": 7900
}
```

El evento contiene:

- Tipo de evento.
- Identificador de cuenta.
- Monto de la operación.
- Fecha y hora.
- Saldo posterior de la cuenta.

El identificador de la cuenta se utiliza como clave del mensaje Kafka, permitiendo mantener los eventos de una misma cuenta asociados a una misma partición.

---

# Backend Core

**Puerto:** `8081`

Backend Core concentra las principales operaciones de negocio relacionadas con cuentas y transacciones.

Es consumido por los tres BFF mediante **OpenFeign**.

Además, Backend Core actúa como **Kafka Producer** para los eventos relacionados con las operaciones de retiro.

Cuando una operación se completa correctamente:

```text
Retiro
  │
  ▼
Actualización de saldo
  │
  ▼
Registro de transacción
  │
  ▼
Evento RETIRO_REALIZADO
  │
  ▼
Kafka
```

## Estructura

```text
backend-core
│
└── src
    └── main
        └── java
            └── com
                └── bancoxyz
                    └── core
                        │
                        ├── BackendCoreApplication.java
                        │
                        ├── controllers
                        │   ├── CuentaController.java
                        │   └── TransaccionController.java
                        │
                        ├── dtos
                        │   ├── CuentaDTO.java
                        │   ├── RetiroDTO.java
                        │   └── TransaccionDTO.java
                        │
                        ├── exceptions
                        │   ├── CuentaNoEncontradaException.java
                        │   ├── GlobalExceptionHandler.java
                        │   ├── MontoInvalidoException.java
                        │   ├── SaldoInsuficienteException.java
                        │   └── TransaccionNoEncontradaException.java
                        │
                        ├── kafka
                        │   ├── KafkaConfig.java
                        │   ├── TransaccionEvento.java
                        │   └── TransaccionProducer.java
                        │
                        ├── model
                        │   ├── Cuenta.java
                        │   └── Transaccion.java
                        │
                        ├── repositories
                        │   ├── CuentaRepository.java
                        │   └── TransaccionRepository.java
                        │
                        └── services
                            ├── CuentaService.java
                            └── TransaccionService.java
```

La carpeta `kafka` contiene los componentes relacionados con la publicación de eventos:

- `KafkaConfig.java`: configuración del productor.
- `TransaccionEvento.java`: estructura del evento.
- `TransaccionProducer.java`: publicación del evento en Kafka.

---

# Auditoria Service

**Puerto principal:** `8085`

`auditoria-service` es un microservicio independiente encargado de consumir y procesar los eventos publicados por Backend Core.

Pertenece al siguiente Consumer Group:

```text
auditoria-group
```

El consumidor escucha el tópico:

```text
transacciones-bancarias
```

Cuando recibe un evento, lo deserializa y procesa como `TransaccionEvento`.

## Estructura

```text
auditoria-service
│
└── src
    └── main
        └── java
            └── com
                └── bancoxyz
                    └── auditoria
                        │
                        ├── AuditoriaServiceApplication.java
                        │
                        ├── config
                        │   └── KafkaConsumerConfig.java
                        │
                        ├── controllers
                        │   └── AuditoriaController.java
                        │
                        └── kafka
                            ├── TransaccionConsumer.java
                            └── TransaccionEvento.java
```

### Componentes principales

`KafkaConsumerConfig.java`

Contiene la configuración del consumidor Kafka y del deserializador JSON.

`TransaccionConsumer.java`

Recibe los eventos mediante `@KafkaListener`.

`TransaccionEvento.java`

Representa la estructura del evento recibido.

`AuditoriaController.java`

Expone un endpoint básico para comprobar el estado del microservicio.

---

# Consumo asíncrono

La comunicación entre Backend Core y Auditoria Service no requiere una llamada HTTP directa.

El flujo es:

```text
Backend Core
     │
     │ publica evento
     ▼
   Kafka
     │
     │ entrega mensaje
     ▼
Auditoria Service
```

Esto permite que Backend Core continúe con su procesamiento sin depender directamente de una respuesta HTTP del servicio de auditoría.

---

# Escalabilidad con Consumer Groups

El tópico `transacciones-bancarias` posee tres particiones.

Para demostrar la escalabilidad se ejecutaron dos instancias de `auditoria-service`, ambas pertenecientes al mismo grupo:

```text
auditoria-group
```

Kafka distribuyó las particiones entre las instancias.

Ejemplo de la asignación realizada:

```text
auditoria-service :8085
    ├── partición 0
    └── partición 1

auditoria-service :8086
    └── partición 2
```

De esta forma, ambas instancias procesan eventos del mismo tópico de manera distribuida.

La incorporación de nuevas instancias permite distribuir las particiones disponibles entre más consumidores del mismo grupo, facilitando el escalamiento horizontal del procesamiento.

---

# Tolerancia a fallos con Resilience4j

Los tres BFF incorporan **Resilience4j** para controlar fallos en las comunicaciones con Backend Core.

Se utilizan:

- Circuit Breaker.
- Retry.
- Backoff exponencial.
- Fallback.
- Timeouts.

El comportamiento general es:

```text
BFF
 │
 ▼
Backend Core
 │
 X
 │
 ▼
Retry
 │
 X
 │
 ▼
Circuit Breaker
 │
 ▼
Fallback
 │
 ▼
HTTP 503
```

Cuando Backend Core no está disponible, el BFF controla el fallo mediante los mecanismos configurados y devuelve una respuesta controlada.

Esta implementación se mantiene en:

- BFF Web.
- BFF Mobile.
- BFF ATM.

---

# BFF Web

**Puerto:** `8082`

El BFF Web proporciona información adaptada al canal web.

Responsabilidades principales:

- Consulta de cuentas.
- Consulta de movimientos.
- Transformación de DTOs.
- Comunicación con Backend Core.
- Manejo de errores.
- Tolerancia a fallos mediante Resilience4j.
- Validación de tokens JWT.

---

# BFF Mobile

**Puerto:** `8083`

El BFF Mobile adapta la información de Backend Core para aplicaciones móviles.

Responsabilidades principales:

- Consulta de cuentas.
- Consulta de transacciones.
- Obtención de movimientos.
- Transformación de información.
- Manejo de errores.
- Tolerancia a fallos mediante Resilience4j.
- Validación de tokens JWT.

---

# BFF ATM

**Puerto:** `8084`

El BFF ATM proporciona las operaciones destinadas al canal de cajeros automáticos.

Responsabilidades principales:

- Consulta de saldo.
- Realización de retiros.
- Validación de operaciones.
- Transformación de respuestas.
- Manejo de errores.
- Tolerancia a fallos mediante Resilience4j.
- Validación de tokens JWT.

---

# Comunicación mediante OpenFeign

Los BFF utilizan **Spring Cloud OpenFeign** para comunicarse con Backend Core.

La comunicación utiliza el nombre lógico:

```text
backend-core
```

Este nombre es resuelto mediante Eureka.

```text
BFF
 │
 │ OpenFeign
 ▼
Eureka
 │
 │ descubre backend-core
 ▼
Backend Core
```

Spring Cloud LoadBalancer permite distribuir las solicitudes entre las instancias disponibles del servicio.

---

# Autenticación con OAuth2 y JWT

La solución incorpora un **Auth Server** utilizando Spring Authorization Server.

**Puerto:**

```text
9000
```

El servidor permite emitir tokens de acceso JWT mediante OAuth2.

El flujo utilizado para las pruebas corresponde a:

```text
client_credentials
```

Los scopes configurados son:

```text
cuentas.read
cuentas.write
```

El token generado se utiliza mediante:

```text
Authorization: Bearer <token>
```

Los BFF funcionan como OAuth2 Resource Servers y validan los tokens JWT antes de permitir el acceso a sus endpoints protegidos.

### Respuestas de seguridad

Sin token:

```text
HTTP 401 Unauthorized
```

Con token inválido:

```text
HTTP 401 Unauthorized
```

Con token válido:

```text
HTTP 200 OK
```

Los tokens utilizados durante las pruebas no se almacenan en el repositorio.

---

# Config Server

**Puerto:** `8888`

El **Config Server** centraliza configuraciones externas para los diferentes servicios.

Se implementó utilizando:

- Spring Cloud Config Server.
- Spring Boot.
- Native configuration repository.

Repositorio local:

```text
C:\config-repo
```

Consulta de configuración:

```text
http://localhost:8888/bff-web/default
```

---

# Discovery Server - Eureka

**Puerto:** `8761`

Dashboard:

```text
http://localhost:8761
```

Los servicios principales registrados incluyen:

```text
BACKEND-CORE    :8081
BFF-WEB         :8082
BFF-MOBILE      :8083
BFF-ATM         :8084
```

El descubrimiento permite utilizar nombres lógicos en lugar de direcciones físicas fijas.

---

# Banco Batch

Banco Batch corresponde al componente encargado del procesamiento de información mediante Spring Batch.

Su responsabilidad principal es procesar información y cargarla en MySQL.

El flujo de datos es:

```text
Archivos / Datos
      │
      ▼
Banco Batch
      │
      ▼
MySQL
      │
      ▼
Backend Core
```

Banco Batch se mantiene como componente independiente dentro de la arquitectura.

---

# Base de datos MySQL

La solución utiliza **MySQL** como sistema de persistencia.

Configuración:

```text
Host: localhost
Puerto: 3307
Base de datos: banco_xyz
```

MySQL se ejecuta mediante Docker.

---

# Infraestructura Kafka mediante Docker Compose

La infraestructura Kafka se encuentra definida en:

```text
docker-compose.kafka.yaml
```

El archivo contiene:

```text
ZooKeeper 1
ZooKeeper 2
ZooKeeper 3

Kafka Broker 1
Kafka Broker 2
Kafka Broker 3

Kafka UI

Kafka Init
```

Puertos externos principales:

```text
Kafka Broker 1: 29092
Kafka Broker 2: 39092
Kafka Broker 3: 49092
Kafka UI:       8090
```

Kafka UI puede utilizarse para visualizar:

- Brokers.
- Topics.
- Particiones.
- Mensajes.
- Consumer Groups.

La infraestructura utiliza volúmenes Docker para mantener la información del clúster entre reinicios normales.

---

# Automatización mediante GitHub Actions

El despliegue de la infraestructura Kafka se automatiza mediante **GitHub Actions**.

Workflow:

```text
.github/
└── workflows/
    └── main.yml
```

El workflow se ejecuta al realizar un `push` sobre la rama:

```text
main
```

El proceso realiza las siguientes acciones:

1. Descarga el repositorio.
2. Configura la conexión SSH.
3. Copia `docker-compose.kafka.yaml` hacia la instancia EC2.
4. Configura la variable `KAFKA_ADVERTISED_HOST`.
5. Valida el archivo Docker Compose.
6. Detiene los contenedores existentes sin eliminar los volúmenes.
7. Descarga las imágenes necesarias.
8. Levanta la infraestructura Kafka.
9. Muestra el estado final de los contenedores.

Las credenciales y datos sensibles utilizados por el workflow se mantienen en **GitHub Secrets**.

Entre las variables utilizadas se encuentran:

```text
EC2_HOST
USER_SERVER
EC2_SSH_KEY
```

El archivo `.env` utilizado en EC2 no se almacena en el repositorio.

---

# Estructura general del repositorio

```text
BancoXYZ
│
├── auth-server
│
├── config-server
│
├── discovery-server
│
├── backend-core
│
├── bff-web
│
├── bff-mobile
│
├── bff-atm
│
├── banco-batch
│
├── auditoria-service
│
├── docker-compose.yaml
│
├── docker-compose.kafka.yaml
│
├── .github
│   └── workflows
│       └── main.yml
│
└── README.md
```

---

# Estructura de Backend Core

```text
backend-core
│
└── src
    └── main
        └── java
            └── com
                └── bancoxyz
                    └── core
                        │
                        ├── BackendCoreApplication.java
                        │
                        ├── controllers
                        │   ├── CuentaController.java
                        │   └── TransaccionController.java
                        │
                        ├── dtos
                        │   ├── CuentaDTO.java
                        │   ├── RetiroDTO.java
                        │   └── TransaccionDTO.java
                        │
                        ├── exceptions
                        │   ├── CuentaNoEncontradaException.java
                        │   ├── GlobalExceptionHandler.java
                        │   ├── MontoInvalidoException.java
                        │   ├── SaldoInsuficienteException.java
                        │   └── TransaccionNoEncontradaException.java
                        │
                        ├── kafka
                        │   ├── KafkaConfig.java
                        │   ├── TransaccionEvento.java
                        │   └── TransaccionProducer.java
                        │
                        ├── model
                        │   ├── Cuenta.java
                        │   └── Transaccion.java
                        │
                        ├── repositories
                        │   ├── CuentaRepository.java
                        │   └── TransaccionRepository.java
                        │
                        └── services
                            ├── CuentaService.java
                            └── TransaccionService.java
```

---

# Estructura de Auditoria Service

```text
auditoria-service
│
└── src
    └── main
        └── java
            └── com
                └── bancoxyz
                    └── auditoria
                        │
                        ├── AuditoriaServiceApplication.java
                        │
                        ├── config
                        │   └── KafkaConsumerConfig.java
                        │
                        ├── controllers
                        │   └── AuditoriaController.java
                        │
                        └── kafka
                            ├── TransaccionConsumer.java
                            └── TransaccionEvento.java
```

---

# Puertos de los servicios

| Componente | Puerto | Función |
|---|---:|---|
| Banco Batch | 8080 | Procesamiento y carga batch |
| Backend Core | 8081 | Lógica central de negocio |
| BFF Web | 8082 | Canal Web |
| BFF Mobile | 8083 | Canal Mobile |
| BFF ATM | 8084 | Canal ATM |
| Auditoria Service | 8085 | Consumo de eventos |
| Auditoria Service — segunda instancia | 8086 | Consumo de eventos |
| Auth Server | 9000 | OAuth2 / JWT |
| Discovery Server | 8761 | Service Discovery |
| Config Server | 8888 | Configuración centralizada |
| Kafka Broker 1 | 29092 | Mensajería |
| Kafka Broker 2 | 39092 | Mensajería |
| Kafka Broker 3 | 49092 | Mensajería |
| Kafka UI | 8090 | Administración Kafka |
| MySQL | 3307 | Persistencia |

---

# Tecnologías utilizadas

## Backend

- Java 21.
- Spring Boot 4.1.1.
- Spring Web.
- Spring Data JPA.
- Spring Security.

## Spring Cloud

- Spring Cloud Config Server.
- Spring Cloud Netflix Eureka.
- Spring Cloud OpenFeign.
- Spring Cloud LoadBalancer.
- Spring Cloud CircuitBreaker.
- Resilience4j.

## Mensajería

- Apache Kafka.
- Spring for Apache Kafka.
- Kafka UI.
- ZooKeeper.

## Seguridad

- Spring Authorization Server.
- OAuth2.
- JWT.
- Spring Security.

## Persistencia

- MySQL.
- Docker.

## Procesamiento

- Spring Batch.

## DevOps

- Docker Compose.
- GitHub Actions.
- Amazon EC2.

## Herramientas

- IntelliJ IDEA.
- Maven.
- Postman.
- Git / GitHub.

---

# Versiones principales

```text
Java                 21
Spring Boot          4.1.1
Spring Cloud         2025.1.3
Spring Kafka         4.1.1
Spring Batch         6.x
MySQL                8.x
Kafka                7.4.4
Docker Compose       5.x
```

---

# Ejecución de la solución

Para ejecutar la arquitectura completa se recomienda iniciar primero los componentes de infraestructura.

### 1. MySQL

Iniciar el contenedor Docker correspondiente a MySQL.

### 2. Config Server

Iniciar:

```text
config-server
```

Puerto:

```text
8888
```

### 3. Discovery Server

Iniciar:

```text
discovery-server
```

Puerto:

```text
8761
```

Verificar:

```text
http://localhost:8761
```

### 4. Backend Core

Iniciar:

```text
backend-core
```

Puerto:

```text
8081
```

### 5. BFFs

Iniciar:

```text
bff-web
bff-mobile
bff-atm
```

Puertos:

```text
8082
8083
8084
```

### 6. Auth Server

Iniciar:

```text
auth-server
```

Puerto:

```text
9000
```

### 7. Kafka

En el servidor EC2:

```bash
docker-compose -f docker-compose.kafka.yaml up -d
```

Verificar:

```bash
docker-compose -f docker-compose.kafka.yaml ps
```

Kafka UI:

```text
http://<EC2_HOST>:8090
```

### 8. Auditoria Service

Iniciar:

```text
auditoria-service
```

Puerto:

```text
8085
```

Para demostrar escalabilidad se puede iniciar una segunda instancia en:

```text
8086
```

Ambas instancias deben utilizar:

```text
auditoria-group
```

---

# Variables de entorno

La configuración de Kafka y Eureka utiliza variables de entorno para evitar almacenar configuraciones dependientes del entorno directamente en el código.

Ejemplo:

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```

Para entornos locales se pueden definir las variables correspondientes en la configuración de ejecución de IntelliJ IDEA.

Las credenciales y datos sensibles no deben almacenarse en el repositorio.

---

# Resultado de la implementación

La arquitectura evolucionó desde una solución basada principalmente en BFF hacia una arquitectura distribuida con capacidades de Spring Cloud, seguridad mediante OAuth2/JWT y procesamiento asíncrono mediante eventos.

Actualmente se cuenta con:

- **3 BFF:** Web, Mobile y ATM.
- **Backend Core** como servicio central de negocio.
- **Eureka Discovery** para descubrimiento de servicios.
- **OpenFeign + LoadBalancer** para comunicación entre microservicios.
- **Resilience4j** para tolerancia a fallos.
- **Auth Server** para emisión de tokens JWT.
- **Spring Security** para protección de los BFF.
- **Config Server** para configuración centralizada.
- **Banco Batch** para procesamiento y carga de información.
- **Apache Kafka** como plataforma de eventos.
- **3 brokers Kafka**.
- **3 particiones** para el tópico `transacciones-bancarias`.
- **Factor de replicación 3**.
- **Backend Core como productor Kafka**.
- **auditoria-service como consumidor Kafka**.
- **Consumer Group `auditoria-group`**.
- Procesamiento asíncrono de eventos `RETIRO_REALIZADO`.
- Escalabilidad mediante múltiples instancias de `auditoria-service`.
- Infraestructura Kafka desplegada mediante **Docker Compose**.
- Despliegue automatizado mediante **GitHub Actions**.

La solución permite combinar comunicación síncrona mediante APIs REST/OpenFeign con comunicación asíncrona mediante eventos Kafka, manteniendo separadas las responsabilidades de los diferentes componentes de la arquitectura.
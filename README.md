# Banco XYZ - Microservicios, Service Discovery, Configuración Centralizada, Resiliencia y Seguridad

## Descripción

Este proyecto corresponde a la evolución de la arquitectura del sistema **Banco XYZ**, incorporando una arquitectura basada en microservicios y capacidades de **Spring Cloud**.

Durante esta etapa se extendió la arquitectura BFF implementada anteriormente, incorporando:

- Configuración centralizada mediante **Spring Cloud Config Server**.
- Descubrimiento de servicios mediante **Netflix Eureka**.
- Balanceo de carga mediante **Spring Cloud LoadBalancer**.
- Comunicación entre servicios mediante **Spring Cloud OpenFeign**.
- Tolerancia a fallos mediante **Resilience4j**.
- Autenticación mediante **Spring Authorization Server**.
- Emisión y validación de **tokens JWT** mediante OAuth2.
- Protección de los tres BFF mediante Spring Security.

La solución mantiene los tres canales de atención existentes:

- BFF Web.
- BFF Mobile.
- BFF ATM.

Además, se mantiene el **Backend Core** como servicio central de negocio y **Banco Batch** como componente encargado del procesamiento y carga de información hacia la base de datos.

---

# Arquitectura de la solución

La arquitectura final implementada se compone de servicios de infraestructura, canales BFF, un servicio central de negocio y componentes de persistencia y procesamiento batch.

```text
                         ┌──────────────────────┐
                         │    CONFIG SERVER     │
                         │        :8888         │
                         │                      │
                         │ Configuración        │
                         │ centralizada         │
                         └──────────┬───────────┘
                                    │
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
                         └────────┬─────────┘
                                  │
                                  ▼
                            ┌───────────┐
                            │   MySQL   │
                            │   :3307   │
                            └───────────┘


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
                               │ Inserción / actualización
                               ▼
                            ┌───────┐
                            │ MySQL │
                            │ :3307 │
                            └───────┘
```

### Flujo general

El funcionamiento de la solución se puede resumir de la siguiente manera:

1. **Banco Batch** procesa los archivos y datos correspondientes a los procesos batch y carga la información en MySQL.
2. **Backend Core** accede a la información almacenada y expone los servicios de negocio.
3. Los **BFF Web, Mobile y ATM** consumen Backend Core mediante OpenFeign.
4. **Eureka Discovery** permite que los BFF encuentren dinámicamente a Backend Core sin depender de una URL fija.
5. **Resilience4j** protege las comunicaciones entre los BFF y Backend Core.
6. **Auth Server** autentica las solicitudes y emite tokens JWT.
7. Los BFF validan el token antes de permitir el acceso a sus endpoints protegidos.
8. **Config Server** centraliza la configuración de los servicios que utilizan configuración externa.

---

# Componentes principales

## Config Server - Puerto 8888

El **Config Server** centraliza configuraciones externas para los diferentes servicios de la arquitectura.

Se implementó utilizando:

- Spring Cloud Config Server.
- Spring Boot.
- Native configuration repository.

Para esta implementación se utilizó un repositorio local de configuración:

```text
C:\config-repo
```

Actualmente contiene configuraciones asociadas a los servicios.

El servidor puede ser consultado mediante:

```text
http://localhost:8888/bff-web/default
```

Este endpoint permite verificar que el Config Server puede entregar la configuración correspondiente al servicio solicitado.

> La infraestructura del Config Server está implementada y validada. La integración automática de consumo desde los microservicios queda como una mejora pendiente si se requiere externalizar completamente la configuración de los BFF.

---

# Discovery Server - Eureka

El **Discovery Server** utiliza Netflix Eureka para permitir el registro y descubrimiento dinámico de los servicios.

Puerto:

```text
8761
```

Dashboard:

```text
http://localhost:8761
```

Los servicios registrados actualmente incluyen:

```text
BACKEND-CORE    :8081
BFF-WEB         :8082
BFF-MOBILE      :8083
BFF-ATM         :8084
```

Esto permite que los BFF encuentren dinámicamente a Backend Core utilizando su nombre lógico:

```text
backend-core
```

En lugar de depender de una dirección fija como:

```text
http://localhost:8081
```

---

# Backend Core

**Puerto:** `8081`

Backend Core concentra las principales operaciones de negocio relacionadas con cuentas y transacciones.

Es consumido por los tres BFF mediante **OpenFeign**.

La comunicación utiliza el nombre registrado en Eureka:

```text
backend-core
```

Esto permite separar el descubrimiento del servicio de su dirección física.

---

# BFF Web

**Puerto:** `8082`

El BFF Web proporciona información adaptada al canal web.

Entre sus responsabilidades se encuentran:

- Consulta de información de cuentas.
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

Entre sus responsabilidades se encuentran:

- Consulta de cuentas.
- Consulta de transacciones.
- Obtención de los últimos movimientos.
- Transformación de información.
- Manejo de errores.
- Tolerancia a fallos mediante Resilience4j.
- Validación de tokens JWT.

---

# BFF ATM

**Puerto:** `8084`

El BFF ATM proporciona las operaciones necesarias para el canal de cajeros automáticos.

Entre sus responsabilidades se encuentran:

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

La configuración utiliza el nombre lógico:

```text
backend-core
```

Este nombre es resuelto mediante Eureka.

La comunicación sigue el siguiente flujo:

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

De esta forma se elimina la dependencia de una URL fija para la comunicación entre servicios.

Además, Spring Cloud LoadBalancer permite seleccionar una instancia disponible del servicio registrado.

---

# Tolerancia a fallos con Resilience4j

Los tres BFF incorporan **Resilience4j** para mejorar la tolerancia a fallos durante la comunicación con Backend Core.

Se implementaron:

- Circuit Breaker.
- Retry.
- Backoff exponencial.
- Fallback.
- Timeouts para las llamadas Feign.

La configuración utiliza una ventana de evaluación para el Circuit Breaker y permite realizar hasta tres intentos antes de considerar que la comunicación está fallando.

El flujo ante una indisponibilidad de Backend Core es:

```text
BFF
 │
 │ solicitud
 ▼
Backend Core
 │
 X servicio no disponible
 │
 ▼
Retry
 │
 X continúa fallando
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

Cuando Backend Core no está disponible, los BFF responden con un error `503 Service Unavailable` y un mensaje indicando que el servicio central no se encuentra disponible.

Esta implementación se encuentra presente en:

- BFF Web.
- BFF Mobile.
- BFF ATM.

---

# Autenticación con OAuth2 y JWT

La solución incorpora un **Auth Server** utilizando Spring Authorization Server.

**Puerto:**

```text
9000
```

El servidor es responsable de emitir tokens de acceso JWT para los clientes autorizados.

La arquitectura de autenticación es:

```text
Cliente / Postman
       │
       │ client_credentials
       ▼
┌─────────────────┐
│   Auth Server   │
│      :9000      │
└────────┬────────┘
         │
         │ JWT
         ▼
┌────────────────────────────┐
│ BFF Web / Mobile / ATM     │
│                            │
│ Spring Security            │
│ JWT Resource Server        │
└────────────┬───────────────┘
             │
             │ OpenFeign
             ▼
       Backend Core
```

El cliente OAuth2 utilizado para las pruebas locales dispone de los scopes:

```text
cuentas.read
cuentas.write
```

El flujo utilizado para obtener un token corresponde a:

```text
client_credentials
```

El token emitido por Auth Server se utiliza posteriormente como:

```text
Authorization: Bearer <token>
```

Los BFF se encuentran protegidos mediante Spring Security y validan los tokens JWT emitidos por el Auth Server.

### Comportamiento de seguridad

Sin token:

```text
HTTP 401 Unauthorized
```

Con un token inválido:

```text
HTTP 401 Unauthorized
```

Con un token válido:

```text
HTTP 200 OK
```

permitiendo acceder al endpoint protegido correspondiente.

Los tokens generados durante las pruebas no se almacenan en el repositorio ni se incluyen en este README.

---

# Banco Batch

El componente **Banco Batch** corresponde al procesamiento de información proveniente de los procesos batch del sistema.

Su responsabilidad principal es procesar y cargar información en la base de datos MySQL.

Los procesos batch existentes trabajan con información relacionada con:

- Transacciones.
- Intereses.
- Estados de cuenta.
- Resúmenes y procesamiento de información bancaria.

El flujo de datos es:

```text
Archivos / Datos de entrada
          │
          ▼
     Banco Batch
          │
          │ procesamiento
          ▼
        MySQL
          │
          ▼
    Backend Core
          │
          ▼
 BFF Web / Mobile / ATM
```

Banco Batch se mantiene como un componente independiente de la arquitectura de microservicios implementada durante esta etapa.

Su función es principalmente la preparación y actualización de información que posteriormente puede ser consultada por Backend Core.

---

# Base de datos MySQL

La solución utiliza **MySQL** como sistema de persistencia.

Configuración utilizada:

```text
Host: localhost
Puerto: 3307
Base de datos: banco_xyz
```

MySQL se ejecuta mediante Docker.

La base de datos es utilizada principalmente por Backend Core y recibe información procesada por Banco Batch.

---

# Estructura del proyecto

La estructura principal del proyecto se organiza de la siguiente manera:

```text
BancoXYZ
│
├── auth-server
│   └── Spring Authorization Server
│
├── config-server
│   └── Spring Cloud Config Server
│
├── discovery-server
│   └── Eureka Server
│
├── backend-core
│   └── Servicio central de negocio
│
├── bff-web
│   └── Canal Web
│
├── bff-mobile
│   └── Canal Mobile
│
├── bff-atm
│   └── Canal ATM
│
├── banco-batch
│   └── Procesamiento y carga batch
│
├── docker-compose.yaml
│
├── README.md
│
└── Evidencias Semana 6.docx
```

Los BFF incorporan las siguientes capas relacionadas con la nueva arquitectura:

```text
bff-web
├── clients
│   └── BackendCoreClient.java
├── config
│   └── SecurityConfig.java
├── exceptions
└── services
    ├── BffWebService.java
    └── BackendCoreResilientService.java
```

La misma estructura conceptual se utiliza en BFF Mobile y BFF ATM.

---

# Puertos de los servicios

| Componente | Puerto | Función |
|---|---:|---|
| Banco Batch | 8080 | Procesamiento y carga batch |
| Backend Core | 8081 | Lógica central de negocio |
| BFF Web | 8082 | Canal web |
| BFF Mobile | 8083 | Canal móvil |
| BFF ATM | 8084 | Canal ATM |
| Auth Server | 9000 | OAuth2 / JWT |
| Discovery Server | 8761 | Service Discovery |
| Config Server | 8888 | Configuración centralizada |
| MySQL | 3307 | Persistencia |

---

# Tecnologías utilizadas

### Backend

- Java 21.
- Spring Boot 4.1.1.
- Spring Web.
- Spring Data JPA.
- Spring Security.

### Spring Cloud

- Spring Cloud Config Server.
- Spring Cloud Netflix Eureka.
- Spring Cloud OpenFeign.
- Spring Cloud LoadBalancer.
- Spring Cloud CircuitBreaker.
- Resilience4j.

### Seguridad

- Spring Authorization Server.
- OAuth2.
- JWT.
- Spring Security.

### Persistencia

- MySQL.
- Docker.

### Procesamiento

- Spring Batch.

### Herramientas

- IntelliJ IDEA.
- Maven.
- Docker.
- Postman.
- Git / GitHub.

---

# Versiones principales

```text
Java                 21
Spring Boot          4.1.1
Spring Cloud         2025.1.3
Spring Batch         6.x
MySQL                8.x
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

Luego se puede verificar el dashboard:

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

El servicio debe registrarse en Eureka.

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

Los tres BFF deben registrarse automáticamente en Eureka.

### 6. Auth Server

Iniciar:

```text
auth-server
```

Puerto:

```text
9000
```

El servidor permitirá solicitar tokens OAuth2 mediante el flujo configurado.

### 7. Banco Batch

Banco Batch puede ejecutarse para realizar los procesos de carga y actualización de información en MySQL.

---

# Nuevas funcionalidades implementadas en esta etapa

La arquitectura incorpora las siguientes capacidades:

### Service Discovery

Los servicios se registran en Eureka y pueden localizarse mediante nombres lógicos.

### Configuración centralizada

Se implementó un servidor Spring Cloud Config para centralizar configuraciones externas.

### Tolerancia a fallos

Los tres BFF incorporan Resilience4j con Retry, Circuit Breaker y fallback.

### Autenticación

Auth Server permite generar tokens JWT utilizando OAuth2.

### Protección de microservicios

Los tres BFF requieren autenticación mediante Bearer Token.

### Balanceo de carga

Las llamadas mediante Feign utilizan Spring Cloud LoadBalancer junto con Eureka.

---

# Funcionalidades pendientes y mejoras futuras

Como trabajo posterior se consideran las siguientes mejoras:

- Completar la integración de **Config Client** en los microservicios que todavía mantienen configuración local.
- Implementar autorización granular utilizando los scopes `cuentas.read` y `cuentas.write` directamente sobre los endpoints.
- Persistir clientes OAuth2 y claves del Auth Server en una solución permanente en lugar de memoria.
- Implementar propagación del token JWT en las comunicaciones internas cuando corresponda.
- Utilizar un sistema seguro para la administración de secretos.
- Incorporar HTTPS para ambientes productivos.
- Externalizar completamente las configuraciones de todos los microservicios.
- Integrar todos los servicios dentro de Docker Compose para simplificar su despliegue.

Estas mejoras permitirían acercar la solución a un escenario productivo.

---

# Resultado de la implementación

La arquitectura evolucionó desde una solución basada principalmente en BFF hacia una arquitectura distribuida con capacidades de Spring Cloud y seguridad.

Actualmente se cuenta con:

- **3 BFF registrados en Eureka:** Web, Mobile y ATM.
- **Backend Core registrado como servicio descubrible.**
- Comunicación BFF → Backend Core mediante **OpenFeign + Eureka + LoadBalancer**.
- **Resilience4j implementado en los tres BFF.**
- Manejo de indisponibilidad de Backend Core mediante respuestas `503`.
- **Auth Server funcional** mediante Spring Authorization Server.
- Emisión de **tokens JWT mediante OAuth2**.
- Protección de los tres BFF mediante Spring Security.
- Rechazo de solicitudes sin autenticación o con tokens inválidos mediante `401 Unauthorized`.
- **Config Server implementado y validado**.
- Banco Batch manteniendo su función de procesamiento y carga de información en MySQL.

Esta implementación permite disponer de una arquitectura más desacoplada, tolerante a fallos, descubrible y protegida, manteniendo la separación por canales proporcionada por los BFF.
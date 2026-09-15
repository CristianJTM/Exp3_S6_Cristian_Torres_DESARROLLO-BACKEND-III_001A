# Banco XYZ - Arquitectura Backend for Frontend (BFF)

## Descripción

Este proyecto implementa una arquitectura basada en el patrón **Backend for Frontend (BFF)** para el sistema bancario simulado **Banco XYZ**.

El objetivo es disponer de diferentes interfaces backend especializadas según el canal utilizado por el cliente:

- **BFF Web:** orientado a clientes que utilizan un navegador web, entregando información completa de las cuentas, resumen de movimientos y movimientos registrados.
- **BFF Mobile:** orientado a dispositivos móviles, entregando información esencial y los últimos movimientos para reducir el volumen de datos transferidos.
- **BFF ATM:** orientado a cajeros automáticos, proporcionando operaciones críticas y esenciales como consulta de saldo y retiros.

Los BFF no acceden directamente a la base de datos. Las operaciones de negocio y el acceso a los datos son centralizados mediante **Backend Core**.

Además, el proyecto incorpora un proceso **Spring Batch** encargado de procesar información proveniente de archivos CSV y poblar la base de datos utilizada posteriormente por Backend Core.

La solución utiliza BFF independientes por canal, permitiendo que cada uno pueda definir sus propios endpoints, DTOs, transformaciones, manejo de errores y optimizaciones según las necesidades del cliente.

---

# Arquitectura

```text
                        ┌─────────────────────┐
                        │     Archivos CSV    │
                        │   bank_legacy_data  │
                        └──────────┬──────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │    Spring Batch     │
                        │       :8080         │
                        └──────────┬──────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │        MySQL        │
                        │    banco_xyz        │
                        │       :3307         │
                        └──────────┬──────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │    Backend Core     │
                        │       :8081         │
                        └──────────┬──────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
             ┌────────────┐ ┌────────────┐ ┌────────────┐
             │  BFF Web   │ │ BFF Mobile │ │  BFF ATM   │
             │    :8082   │ │    :8083   │ │    :8084   │
             └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
                   │              │              │
                   ▼              ▼              ▼
               Cliente Web     Cliente Mobile   Cajero ATM
```

## Flujo de comunicación

Los clientes se comunican exclusivamente con el BFF correspondiente.

```text
Cliente
   │
   ▼
BFF específico
   │
   │ HTTP/REST + OpenFeign
   ▼
Backend Core
   │
   ▼
MySQL
```

Los BFF no poseen acceso directo a la base de datos y tampoco contienen las reglas principales del negocio.

La responsabilidad de cada componente se mantiene separada:

- **Spring Batch:** procesamiento y carga de datos.
- **Backend Core:** acceso a datos y reglas de negocio.
- **BFF:** adaptación, transformación y agregación de información según el canal.

---

# Integración y agregación de información

Los BFF integran información proveniente de distintos recursos/endpoints del **Backend Core** y la agregan mediante servicios propios, transformándola en respuestas específicas para cada canal.

Por ejemplo, el **BFF Web** obtiene información de la cuenta mediante:

```text
GET /api/cuentas/{cuentaId}
```

y los movimientos mediante:

```text
GET /api/transacciones/cuenta/{cuentaId}
```

Posteriormente, `BffWebService` combina esta información para construir una respuesta que contiene:

- Identificador de cuenta.
- Saldo.
- Total de movimientos.
- Total de depósitos.
- Total de retiros.
- Detalle de los movimientos.

El **BFF Mobile** también integra información de la cuenta y sus transacciones, pero aplica una transformación diferente, entregando únicamente:

- Identificador de cuenta.
- Saldo.
- Últimos cinco movimientos.

El **BFF ATM**, por su parte, utiliza los recursos necesarios para sus operaciones críticas, principalmente consulta de cuenta y retiro.

De esta manera, los BFF no simplemente exponen nuevamente los endpoints del Backend Core, sino que **coordinan, agregan y transforman información para construir contratos específicos para cada canal**.

---

# Estructura del proyecto

Todos los componentes se encuentran dentro del mismo repositorio:

```text
banco-xyz-bff/
│
├── docker-compose.yaml
├── README.md
├── Evidencias Semana 5.docx
│
├── banco-batch/
│   ├── data/
│   │   ├── semana_1/
│   │   ├── semana_2/
│   │   └── semana_3/
│   ├── src/
│   └── pom.xml
│
├── backend-core/
│   ├── src/
│   └── pom.xml
│
├── bff-web/
│   ├── src/
│   └── pom.xml
│
├── bff-mobile/
│   ├── src/
│   └── pom.xml
│
└── bff-atm/
    ├── src/
    └── pom.xml
```

Cada BFF posee su propia aplicación Spring Boot, configuración, controladores, clientes HTTP, DTOs, excepciones y servicios.

---

# Organización interna de los componentes

## Backend Core

```text
backend-core/
└── src/main/java/com/bancoxyz/core/
    ├── controllers/
    ├── dtos/
    ├── exceptions/
    ├── model/
    ├── repositories/
    └── services/
```

Backend Core mantiene separadas las responsabilidades relacionadas con:

- Controladores REST.
- DTOs.
- Entidades.
- Repositorios.
- Servicios.
- Manejo de excepciones.

---

## Spring Batch

```text
banco-batch/
└── src/main/java/com/bancoxyz/batch/
    ├── config/
    ├── controller/
    ├── exception/
    ├── listener/
    ├── model/
    ├── processor/
    ├── repository/
    ├── service/
    ├── tasklet/
    └── writer/
```

La estructura permite separar la configuración de los jobs, procesamiento, listeners, repositorios, servicios y writers.

---

## BFF Web

```text
bff-web/
└── src/main/java/com/bancoxyz/bff/web/
    ├── clients/
    ├── controllers/
    ├── dtos/
    │   └── core/
    ├── exceptions/
    └── services/
```

El BFF Web utiliza DTOs propios para evitar exponer directamente los modelos del Backend Core.

---

## BFF Mobile

```text
bff-mobile/
└── src/main/java/com/bancoxyz/bff/mobile/
    ├── clients/
    ├── controllers/
    ├── dtos/
    │   └── core/
    ├── exceptions/
    └── services/
```

El BFF Mobile posee sus propios DTOs y lógica de transformación orientada a respuestas más livianas.

---

## BFF ATM

```text
bff-atm/
└── src/main/java/com/bancoxyz/bff/atm/
    ├── clients/
    ├── controllers/
    ├── dtos/
    │   └── core/
    ├── exceptions/
    └── services/
```

El BFF ATM incorpora además DTOs específicos para las solicitudes y respuestas de retiro.

---

# Microservicios y puertos

| Servicio | Puerto | Función |
|---|---:|---|
| Banco Batch | `8080` | Procesamiento y carga de datos |
| Backend Core | `8081` | Lógica de negocio y acceso a datos |
| BFF Web | `8082` | Backend especializado para clientes Web |
| BFF Mobile | `8083` | Backend especializado para clientes Mobile |
| BFF ATM | `8084` | Backend especializado para cajeros automáticos |
| MySQL | `3307` | Persistencia de datos |

---

# Tecnologías utilizadas

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Batch
- Spring Cloud OpenFeign
- MySQL
- Maven
- Docker
- Docker Compose
- REST API

---

# Requisitos previos

Antes de ejecutar el proyecto se requiere tener instalado:

- Java 21
- Docker
- Docker Compose
- Maven (opcional, ya que cada proyecto incluye Maven Wrapper)

---

# Puesta en marcha

El orden de ejecución es importante debido a las dependencias entre los componentes.

## 1. Levantar la base de datos

Desde la raíz del proyecto ejecutar:

```bash
docker-compose up -d
```

Esto inicia el contenedor MySQL utilizado por la aplicación.

La base de datos utilizada es:

```text
banco_xyz
```

El puerto utilizado desde el equipo local es:

```text
3307
```

---

# 2. Levantar Banco Batch

Ingresar al directorio:

```bash
cd banco-batch
```

Ejecutar:

```bash
mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

El servicio estará disponible en:

```text
http://localhost:8080
```

## Procesar los datos

Una vez iniciado Banco Batch, se puede ejecutar el procesamiento completo mediante:

```text
GET http://localhost:8080/api/batch/procesar
```

También se encuentra disponible el procesamiento de estados anuales:

```text
GET http://localhost:8080/api/batch/estados-anuales
```

Los archivos CSV utilizados se encuentran en:

```text
banco-batch/data/
```

organizados por semana:

```text
data/
├── semana_1/
├── semana_2/
└── semana_3/
```

El procesamiento permite preparar y almacenar la información necesaria para el funcionamiento posterior de Backend Core.

---

# 3. Levantar Backend Core

Ingresar al directorio:

```bash
cd backend-core
```

Ejecutar:

```bash
mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

Backend Core estará disponible en:

```text
http://localhost:8081
```

**Backend Core debe estar ejecutándose para utilizar cualquiera de los BFF.**

---

# 4. Levantar BFF Web

Ingresar al directorio:

```bash
cd bff-web
```

Ejecutar:

```bash
mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

Disponible en:

```text
http://localhost:8082
```

---

# 5. Levantar BFF Mobile

Ingresar al directorio:

```bash
cd bff-mobile
```

Ejecutar:

```bash
mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

Disponible en:

```text
http://localhost:8083
```

---

# 6. Levantar BFF ATM

Ingresar al directorio:

```bash
cd bff-atm
```

Ejecutar:

```bash
mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

Disponible en:

```text
http://localhost:8084
```

---

# Endpoints de Backend Core

Backend Core concentra el acceso a la información y las principales reglas de negocio.

## Consultar cuenta

```http
GET http://localhost:8081/api/cuentas/101
```

Este endpoint permite obtener la información de una cuenta.

---

## Consultar transacciones de una cuenta

```http
GET http://localhost:8081/api/transacciones/cuenta/101
```

Este endpoint permite obtener las transacciones asociadas a una cuenta.

La respuesta contiene información como:

- Identificador.
- Fecha.
- Monto.
- Tipo de transacción.
- Cuenta asociada.

---

## Realizar retiro

```http
POST http://localhost:8081/api/cuentas/101/retiros
```

Body:

```json
{
    "monto": 2500
}
```

Backend Core valida la operación, actualiza el saldo y registra la transacción correspondiente.

Las validaciones principales incluyen:

- Existencia de la cuenta.
- Monto mayor que cero.
- Saldo suficiente.

---

# BFF Web

El BFF Web adapta la información de Backend Core para clientes web que requieren mayor cantidad de información.

## Detalle de cuenta

```http
GET http://localhost:8082/api/web/cuentas/101
```

La respuesta contiene información agregada de la cuenta y sus movimientos.

Incluye:

- Identificador de cuenta.
- Saldo.
- Resumen de movimientos.
- Total de depósitos.
- Total de retiros.
- Total de movimientos.
- Detalle de movimientos.

Para construir esta respuesta, el BFF Web consulta información de la cuenta y las transacciones mediante Backend Core y posteriormente las combina mediante `BffWebService`.

---

## Resumen de cuenta

```http
GET http://localhost:8082/api/web/cuentas/101/resumen
```

Este endpoint entrega una respuesta resumida con los principales indicadores de la cuenta.

---

# BFF Mobile

El BFF Mobile está diseñado para entregar una respuesta más liviana y adecuada para dispositivos móviles.

## Consultar cuenta

```http
GET http://localhost:8083/api/mobile/cuentas/101
```

La respuesta contiene:

- Identificador de cuenta.
- Saldo.
- Últimos cinco movimientos.

Los movimientos se ordenan desde el más reciente al más antiguo.

El BFF consulta la información de la cuenta y sus transacciones desde Backend Core y posteriormente aplica las transformaciones necesarias para construir `CuentaMobileDTO`.

Esta estrategia permite reducir el volumen de información entregado al dispositivo móvil.

---

# BFF ATM

El BFF ATM está diseñado para operaciones simples, críticas y eficientes de un cajero automático.

Las operaciones implementadas son:

- Consulta de saldo.
- Retiro de dinero.

## Consulta de saldo

```http
GET http://localhost:8084/api/atm/cuentas/101
```

La respuesta contiene únicamente la información necesaria para la consulta:

```json
{
    "cuentaId": 101,
    "saldo": 0
}
```

> El valor del saldo dependerá de los datos procesados y de las operaciones realizadas durante la ejecución.

---

## Realizar retiro

```http
POST http://localhost:8084/api/atm/cuentas/101/retiros
```

Body:

```json
{
    "monto": 2500
}
```

La respuesta es específica para el canal ATM e incluye información como:

```json
{
    "cuentaId": 101,
    "montoRetirado": 2500,
    "saldo": 0,
    "estado": "APROBADO"
}
```

> El saldo mostrado dependerá del estado actual de la cuenta.

El flujo de la operación es:

```text
Cliente ATM
     │
     ▼
BFF ATM
     │
     │ OpenFeign
     ▼
Backend Core
     │
     ▼
Validación de negocio
     │
     ▼
Actualización de cuenta
     │
     ▼
Registro de transacción
     │
     ▼
BFF ATM
     │
     ▼
Respuesta adaptada
```

---

# Comunicación mediante OpenFeign

Los BFF utilizan **Spring Cloud OpenFeign** para comunicarse con Backend Core.

Cada BFF posee su propio `BackendCoreClient`.

Por ejemplo, el BFF Web y Mobile consumen recursos de cuenta y transacciones:

```text
GET /api/cuentas/{cuentaId}
GET /api/transacciones/cuenta/{cuentaId}
```

Mientras que el BFF ATM utiliza los recursos necesarios para sus operaciones:

```text
GET /api/cuentas/{cuentaId}
POST /api/cuentas/{cuentaId}/retiros
```

Esto permite que cada BFF mantenga su propia lógica de coordinación y transformación, sin acceder directamente a la base de datos.

---

# Integración y transformación de respuestas

Los BFF utilizan DTOs específicos para evitar exponer directamente los DTOs utilizados internamente por Backend Core.

La estructura general es:

```text
Backend Core
     │
     │ CuentaCoreDTO
     │ TransaccionCoreDTO
     ▼
BFF
     │
     │ Transformación
     ▼
DTO específico del canal
```

Por ejemplo:

```text
CuentaCoreDTO
      +
TransaccionCoreDTO
      │
      ▼
BffWebService
      │
      ▼
CuentaWebDetalleDTO
```

Para Mobile:

```text
CuentaCoreDTO
      +
TransaccionCoreDTO
      │
      ▼
BffMobileService
      │
      ├── ordenar
      ├── limitar a 5
      └── transformar
      ▼
CuentaMobileDTO
```

Para ATM:

```text
CuentaCoreDTO
      │
      ▼
BffAtmService
      │
      ▼
CuentaAtmDTO
```

En el caso de los retiros, el BFF ATM además transforma la respuesta de Backend Core en:

```text
RetiroAtmResponseDTO
```

---

# Manejo de errores

Los BFF implementan manejo de errores para evitar exponer directamente errores internos de Backend Core al cliente.

## Cuenta no encontrada

Cuando se consulta una cuenta inexistente, el BFF transforma el error en una respuesta:

```text
HTTP 404
```

con información indicando que la cuenta no fue encontrada.

---

## Monto inválido

En las operaciones de retiro, un monto igual o menor que cero genera:

```text
HTTP 400
```

con el error:

```text
Monto inválido
```

---

## Saldo insuficiente

Si el monto solicitado supera el saldo disponible:

```text
HTTP 400
```

con el error:

```text
Saldo insuficiente
```

---

## Backend Core no disponible

Los BFF también controlan los problemas de comunicación con Backend Core.

Cuando no es posible comunicarse con el servicio dependiente, se entrega:

```text
HTTP 503
```

con información similar a:

```json
{
    "status": 503,
    "error": "Backend Core no disponible",
    "message": "No fue posible comunicarse con el Backend Core"
}
```

De esta forma, el cliente puede distinguir entre un problema de negocio y un problema de disponibilidad del servicio.

---

# Configuración de tiempos de espera

Los BFF utilizan configuración de timeout para controlar el tiempo de espera de las llamadas realizadas mediante OpenFeign.

La configuración utilizada es:

```properties
spring.cloud.openfeign.client.config.backend-core.connectTimeout=2000
spring.cloud.openfeign.client.config.backend-core.readTimeout=5000
```

Además, cada BFF utiliza la URL configurada para Backend Core:

```properties
backend-core.url=http://localhost:8081
```

Los tiempos de espera permiten evitar esperas indefinidas cuando Backend Core no responde y contribuyen a controlar el consumo de conexiones y recursos.

---

# Persistencia

La aplicación utiliza MySQL mediante Docker Compose.

Las principales entidades utilizadas por Backend Core son:

```text
cuentas
transacciones
```

Spring Batch es responsable de procesar los archivos CSV y generar la información utilizada posteriormente por Backend Core.

Los BFF no tienen acceso directo a estas tablas.

---

# Consideraciones sobre los datos

Los datos utilizados corresponden a información bancaria simulada proveniente de archivos CSV.

Para las funcionalidades utilizadas por los BFF se consideran principalmente las relaciones entre:

```text
cuentas
    │
    └── transacciones
```

El archivo `cuentas_anuales.csv` contiene `cuenta_id` asociado a los movimientos, por lo que permite relacionar la información utilizada para las funcionalidades de cuentas y transacciones.

El archivo `transacciones.csv` corresponde a un flujo independiente utilizado por el procesamiento Batch y no contiene `cuenta_id`, por lo que no se utiliza para relacionar directamente los movimientos mostrados por los BFF.

El archivo `intereses.csv` presenta inconsistencias en la relación entre cuentas y nombres, por lo que los intereses procesados no forman parte de las funcionalidades implementadas por los BFF.

Además, los archivos de datos no proporcionan un saldo bancario inicial explícito. Para esta implementación, el saldo utilizado en `cuentas.saldo` corresponde al movimiento neto calculado durante el procesamiento Batch, permitiendo simular las operaciones de consulta y retiro requeridas.

---

# Arquitectura BFF implementada

La solución utiliza BFF independientes y especializados para cada canal.

| Característica | Web | Mobile | ATM |
|---|:---:|:---:|:---:|
| Consulta de saldo | ✓ | ✓ | ✓ |
| Resumen de movimientos | ✓ | - | - |
| Movimientos completos | ✓ | - | - |
| Últimos movimientos | - | ✓ | - |
| Retiro | - | - | ✓ |
| Respuesta específica por canal | ✓ | ✓ | ✓ |
| Transformación mediante DTO propio | ✓ | ✓ | ✓ |
| Integración con Backend Core | ✓ | ✓ | ✓ |
| Acceso directo a BD | ✗ | ✗ | ✗ |

---

# Organización y escalabilidad

Cada BFF mantiene su propia estructura de:

```text
clients
controllers
dtos
exceptions
services
```

Esta organización permite separar:

- Exposición de endpoints.
- Comunicación con Backend Core.
- Modelos de respuesta.
- Manejo de errores.
- Lógica de coordinación y transformación.

Al tratarse de BFF independientes, cada canal puede evolucionar, modificar sus respuestas y aplicar optimizaciones sin afectar directamente a los otros canales.

Los componentes técnicos de cada BFF también mantienen responsabilidades separadas, evitando acoplar la lógica de un canal con otro.

---

# Seguridad

La implementación actual está enfocada en la construcción y validación funcional del patrón **Backend for Frontend**.

Como mejora futura se contempla incorporar:

- Autenticación.
- Autorización basada en roles y permisos.
- Tokens de acceso.
- Seguridad específica para cada canal.
- Protección adicional de las operaciones críticas del BFF ATM.

Estas funcionalidades pueden incorporarse posteriormente mediante Spring Security y mecanismos de autenticación basados en tokens.

---

# Resultado

La solución implementa tres BFF independientes, especializados según las necesidades de cada canal:

```text
                         Backend Core
                              │
               ┌──────────────┼──────────────┐
               │              │              │
               ▼              ▼              ▼
            BFF Web       BFF Mobile       BFF ATM
               │              │              │
               ▼              ▼              ▼
           Cliente Web    Cliente Mobile   Cajero ATM
```

Cada BFF integra información desde Backend Core, la transforma mediante servicios y DTOs propios y entrega una respuesta adaptada al canal correspondiente.

El resultado permite aplicar el patrón **Backend for Frontend** manteniendo:

- Independencia entre canales.
- Separación de responsabilidades.
- Integración y agregación de información.
- Respuestas específicas por cliente.
- Optimización de datos transferidos.
- Manejo de errores.
- Control de tiempos de espera.
- Organización modular y escalable.

De esta manera, Web, Mobile y ATM no necesitan consumir directamente la estructura interna de Backend Core, sino que disponen de interfaces backend especializadas de acuerdo con sus necesidades.
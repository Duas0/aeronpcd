# AERON - Simulador de Aeropuerto (PCD)

> **Asignatura:** Programación Concurrente y Distribuida  
> **Lenguaje:** Java 8+  
> **Versión:** Final Release

## Descripción del Proyecto

Este proyecto implementa un simulador de tráfico aéreo completo para el aeropuerto **AERON**. El sistema gestiona el ciclo de vida de **20 aviones** que compiten en tiempo real por recursos limitados (**3 pistas** de aterrizaje/despegue y **5 puertas** de embarque).

El simulador está diseñado para operar bajo dos paradigmas de ejecución distintos, permitiendo comparar el rendimiento y la lógica de programación:

1.  **Modo Secuencial:** Ejecución lineal estricta (FIFO). Los aviones operan uno tras otro sin competencia real.
2.  **Modo Concurrente:** Ejecución paralela real utilizando **Hilos (Threads)**, mecanismos de sincronización (**Semáforos**, **Monitores**) y algoritmos de prevención de interbloqueos.
---

## Justificación de Diseño: Problemas Clásicos
La arquitectura del simulador integra soluciones a problemas clásicos de concurrencia para garantizar la eficiencia, la integridad de los datos y la estabilidad del sistema ante la competencia por recursos.

1. **Productor-Consumidor (Torre de Control)**
Se aplica este patrón para gestionar la comunicación entre los aviones y la torre. Dado que el flujo de llegada de las aeronaves es estocástico y puede presentar ráfagas de actividad, se utiliza una cola intermedia (requestQueue) para desacoplar la solicitud del procesamiento.

Implementación: Los aviones (Productores) depositan sus peticiones en la cola y quedan en espera pasiva, mientras que los operarios (Consumidores) procesan las solicitudes de forma lineal. Esto evita que los aviones se bloqueen mutuamente esperando atención inmediata y garantiza que ninguna petición se pierda ante picos de tráfico (Buffer).

2. **Lectores-Escritores (Panel de Vuelos JSON)**
Este esquema se utiliza para gestionar el acceso al fichero de estado del aeropuerto. El Panel de Vuelos es un recurso de consulta masiva (lectura frecuente por pasajeros/GUI) pero de actualización puntual (escritura exclusiva por la Torre).

Implementación: Se emplea un ReentrantReadWriteLock para maximizar el rendimiento de lectura, permitiendo que múltiples procesos consulten el estado simultáneamente sin bloquearse. El bloqueo total solo se aplica durante los breves instantes de escritura, asegurando la integridad de los datos y evitando lecturas de estados inconsistentes o corruptos.

3. **La Cena de los Filósofos (Gestión de Pistas y Puertas)**
La maniobra de aterrizaje representa el punto crítico donde un proceso requiere la tenencia simultánea de dos recursos compartidos finitos (Pista y Puerta) para avanzar, un escenario propenso a Interbloqueos (Deadlocks) si se aplicara una reserva secuencial ("Retener y Esperar").

Implementación: Se aplica una estrategia de Todo o Nada. La torre actúa como un árbitro que verifica el estado global y solo concede permiso si ambos recursos están disponibles al mismo tiempo. Si no es posible satisfacer la demanda completa, el avión no retiene ningún recurso y pasa a una cola de espera, eliminando la posibilidad de bloqueo circular del sistema.
---

##  Estructura del Proyecto

El código fuente se organiza siguiendo una arquitectura modular por paquetes:

* **`src/aeronpcd`**
    * **`main`**
        * `Main.java`: Punto de entrada. Configura el entorno, valida reglas y lanza la simulación.
    * **`model`**
        * `Passenger.java`: Entidad pasiva para simular la carga de pasaje.
        * **`.concurrent`**
            * `ControlTowerConcurrent.java`: Lógica compleja con gestión de hilos y colas de espera.
            * `Plane.java`: Hilo que representa la entidad Avión y su ciclo de vida.
        * **`.sequential`**
            * `ControlTowerSequential.java`: Lógica lineal simple.
            * `PlaneSequential.java`: Versión del avión para ejecución en serie.
    * **`util`**
        * `Runway.java`: Recurso compartido (Pista).
        * `Gate.java`: Recurso compartido (Puerta).
        * `GestorJSON.java`: Persistencia del estado (Panel de Vuelos).
        * `SimulationLogger.java`: Sistema de registro de logs en disco.
        * `EstadisticasVuelo.java`: Generador de informes CSV.
        * `Ventana.java`: Interfaz Gráfica (GUI) con Swing.
        * `AirportState.java`: Utilidad de formateo visual (ASCII Art).
    * **`exceptions`**
        * `AeronException`: Base de errores.
        * `SaturationException`, `LogException`, `ResourceException`, etc.

---

## Guía de Instalación y Ejecución

### Requisitos Previos
* Java Development Kit (JDK) 8 o superior instalado.

### 1. Compilación
Abre una terminal en la carpeta raíz del proyecto y ejecuta:
```bash
javac -d bin -sourcepath src src/main/Main.java
```
### 1. Ejecución
1. Para ejecutar en Modo Secuencial:
```bash
java -cp bin main.Main SEQUENTIAL
```
2. Para ejecutar en Modo Concurrente:
```bash
java -cp bin main.Main CONCURRENT
```
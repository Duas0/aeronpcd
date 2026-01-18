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
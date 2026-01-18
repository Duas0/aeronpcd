package main;

import exceptions.AeronConfigException;
import model.concurrent.ControlTowerConcurrent;
import model.concurrent.Plane;
import model.sequential.ControlTowerSequential;
import model.sequential.PlaneSequential;
import util.EstadisticasVuelo;
import util.SimulationLogger;
import util.Ventana;

/**
 * Clase principal de entrada para la simulación del Aeropuerto AERON.
 * <p>
 * Responsabilidades:
 * <ul>
 * <li>Leer argumentos de entrada para seleccionar el modo (SEQUENTIAL /
 * CONCURRENT).</li>
 * <li>Configurar los parámetros iniciales de la simulación.</li>
 * <li>Validar antes de iniciar las variables necesarias
 * ({@link AeronConfigException}).</li>
 * <li>Inicializar los sistemas del paquete util (Logger, CSV, Ventana).</li>
 * <li>Lanzar la simulación.</li>
 * </ul>
 * </p>
 */
public class Main {

    /**
     * Modo por defecto si no se pasa argumento.
     */
    private static String MODE = "CONCURRENT";

    private static final int NUM_AVIONES = 20;
    private static final int NUM_PISTAS = 3;
    private static final int NUM_PUERTAS = 5;

    /**
     * Número de operarios en la torre 
     */
    private static final int NUM_OPERARIOS = 5;

    // =============================================================
    /**
     * Método principal.
     *
     * @param args Puede recibir un argumento: "SEQUENTIAL" o "CONCURRENT".
     */
    public static void main(String[] args) {
        try {
            // 0. Procesar Argumentos
            procesarArgumentos(args);

            // 1. Validación de reglas
            validarConfiguracion();

            // 2. Inicialización de sistemas de registro
            SimulationLogger.setup(MODE, NUM_AVIONES, NUM_PISTAS, NUM_PUERTAS, NUM_OPERARIOS);
            EstadisticasVuelo.setup();

            // 3. Inicialización de Ventana
            Ventana ventana = new Ventana();

            System.out.println("--------------------------------------------------");
            System.out.println("INICIANDO AERON SIMULATOR");
            System.out.println("MODO ACTIVO: " + MODE);
            System.out.println("--------------------------------------------------");

            // 4. Ejecución según el modo seleccionado
            if (MODE.equalsIgnoreCase("CONCURRENT")) {
                ejecutarModoConcurrente(ventana);
            } else {
                ejecutarModoSecuencial(ventana);
            }

            // 5. Registro para asegurar guardado de ficheros
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cerrando recursos y guardando logs...");
                SimulationLogger.close();
                EstadisticasVuelo.close();
            }));

        } catch (AeronConfigException e) {
            System.err.println("ERROR FATAL DE CONFIGURACIÓN:");
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR INESPERADO:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Lee y valida los argumentos de la línea de comandos.
     */
    private static void procesarArgumentos(String[] args) {
        if (args.length > 0) {
            String input = args[0].toUpperCase();
            if (input.equals("SEQUENTIAL") || input.equals("CONCURRENT")) {
                MODE = input;
            } else {
                System.out.println("Argumento no reconocido: '" + args[0] + "'.");
                System.out.println("Usando modo por defecto: " + MODE);
                System.out.println("Uso: java main.Main [SEQUENTIAL | CONCURRENT]");
            }
        }
    }

    /**
     * Lanza la simulación usando hilos, semáforos y monitores.
     */
    private static void ejecutarModoConcurrente(Ventana ventana) {
        ControlTowerConcurrent tower = new ControlTowerConcurrent(NUM_PISTAS, NUM_PUERTAS, ventana);
        tower.startOperators(NUM_OPERARIOS);

        ventana.setTower(tower);
        ventana.updateResources(); // Estado inicial

        for (int i = 1; i <= NUM_AVIONES; i++) {
            String planeId = String.format("IBE-%03d", i);
            Plane p = new Plane(planeId, tower, ventana);
            p.start();
            // Pequeña pausa para escalonar las llegadas
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Lanza la simulación usando una lógica secuencial FIFO estricta.
     */
    private static void ejecutarModoSecuencial(Ventana ventana) {
        ControlTowerSequential tower = new ControlTowerSequential(NUM_PISTAS, NUM_PUERTAS, ventana);

        ventana.setTower(tower);
        ventana.updateResources();

        for (int i = 1; i <= NUM_AVIONES; i++) {
            String planeId = String.format("IBE-%03d", i);
            PlaneSequential p = new PlaneSequential(planeId, tower, ventana);
            p.start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Valida los parámetros de configuración antes de iniciar nada.
     *
     * @throws AeronConfigException Si alguna regla de negocio es violada.
     */
    private static void validarConfiguracion() throws AeronConfigException {
        if (NUM_AVIONES < 1) {
            throw new AeronConfigException("Debe haber al menos 1 avión para simular.");
        }
        if (NUM_PISTAS < 1) {
            throw new AeronConfigException("El aeropuerto debe tener al menos 1 pista.");
        }
        if (NUM_PUERTAS < 1) {
            throw new AeronConfigException("El aeropuerto debe tener al menos 1 puerta de embarque.");
        }
        if (NUM_OPERARIOS < 1 && MODE.equalsIgnoreCase("CONCURRENT")) {
            throw new AeronConfigException("En modo concurrente debe haber al menos 1 operario.");
        }
        if (NUM_PISTAS > NUM_PUERTAS) {
            throw new AeronConfigException("Configuración ilógica: Más pistas que puertas de embarque.");
        }
    }
}

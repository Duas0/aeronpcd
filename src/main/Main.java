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
 * <li>Leer argumentos de entrada dinámicos Modo, Aviones, Pistas, Puertas,
 * Operarios.</li>
 * <li>Configurar los parámetros iniciales de la simulación.</li>
 * <li>Validar las reglas de negocio antes de iniciar
 * ({@link AeronConfigException}).</li>
 * <li>Inicializar los sistemas de soporte (Logger, CSV, Ventana).</li>
 * <li>Lanzar la simulación.</li>
 * </ul>
 * </p>
 */
public class Main {

    // Valores por defecto (Configuración inicial)
    private static String MODE = "CONCURRENT";
    private static int NUM_AVIONES = 20;
    private static int NUM_PISTAS = 3;
    private static int NUM_PUERTAS = 5;
    private static int NUM_OPERARIOS = 5;

    // =============================================================
    /**
     * Método principal. Acepta argumentos en orden: [MODO] [AVIONES] [PISTAS]
     * [PUERTAS] [OPERARIOS]
     */
    public static void main(String[] args) {
        try {
            // 0. Procesar Argumentos de Línea de Comandos
            procesarArgumentos(args);

            // 1. Validación de reglas de negocio
            validarConfiguracion();

            // 2. Inicialización de sistemas de registro
            SimulationLogger.setup(MODE, NUM_AVIONES, NUM_PISTAS, NUM_PUERTAS, NUM_OPERARIOS);
            EstadisticasVuelo.setup();

            // 3. Inicialización de la Interfaz Gráfica
            Ventana ventana = new Ventana();

            System.out.println("--------------------------------------------------");
            System.out.println("INICIANDO AERON SIMULATOR");
            System.out.printf("MODO: %s | AVIONES: %d | PISTAS: %d | PUERTAS: %d | OPERARIOS: %s%n",
                    MODE, NUM_AVIONES, NUM_PISTAS, NUM_PUERTAS,
                    MODE.equals("CONCURRENT") ? NUM_OPERARIOS : "N/A");
            System.out.println("--------------------------------------------------");

            // 4. Ejecución según el modo seleccionado
            if (MODE.equalsIgnoreCase("CONCURRENT")) {
                ejecutarModoConcurrente(ventana);
            } else {
                ejecutarModoSecuencial(ventana);
            }

            // 5. Registro de gancho de cierre para asegurar guardado de ficheros
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
     * Lee y valida los argumentos de la línea de comandos. Formato esperado:
     * java main.Main [MODO] [AVIONES] [PISTAS] [PUERTAS] [OPERARIOS]
     */
    private static void procesarArgumentos(String[] args) {
        if (args.length > 0) {
            // 1. MODO
            String inputMode = args[0].toUpperCase();
            if (inputMode.equals("SEQUENTIAL") || inputMode.equals("CONCURRENT")) {
                MODE = inputMode;
            } else {
                System.out.println("Argumento de modo no reconocido: '" + args[0] + "'. Usando " + MODE);
            }

            try {
                // 2. AVIONES
                if (args.length >= 2) {
                    NUM_AVIONES = Integer.parseInt(args[1]);
                }

                // 3. PISTAS
                if (args.length >= 3) {
                    NUM_PISTAS = Integer.parseInt(args[2]);
                }

                // 4. PUERTAS
                if (args.length >= 4) {
                    NUM_PUERTAS = Integer.parseInt(args[3]);
                }

                // 5. OPERARIOS (Solo si es concurrente, aunque se puede leer siempre)
                if (args.length >= 5) {
                    NUM_OPERARIOS = Integer.parseInt(args[4]);
                }

            } catch (NumberFormatException e) {
                System.err.println("Error al leer parámetros numéricos. Usando valores por defecto.");
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
        ventana.updateResources(); // Estado inicial visual

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
            throw new AeronConfigException("Debe haber al menos 1 avión.");
        }
        if (NUM_PISTAS < 1) {
            throw new AeronConfigException("Debe haber al menos 1 pista.");
        }
        if (NUM_PUERTAS < 1) {
            throw new AeronConfigException("Debe haber al menos 1 puerta.");
        }

        if (MODE.equalsIgnoreCase("CONCURRENT") && NUM_OPERARIOS < 1) {
            throw new AeronConfigException("En modo concurrente debe haber al menos 1 operario.");
        }
        if (NUM_PISTAS > NUM_PUERTAS) {
            throw new AeronConfigException("Configuración ilógica: Más pistas (" + NUM_PISTAS + ") que puertas (" + NUM_PUERTAS + ").");
        }
    }
}

package util;

import exceptions.LogException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Gestor centralizado de registros (Logs) para la simulación AERON.
 * <p>
 * Esta clase es responsable de la persistencia de la traza de ejecución en
 * ficheros de texto. Organiza los ficheros en directorios separados según el
 * modo de ejecución (Secuencial o Concurrente).
 * </p>
 */
public class SimulationLogger {

    private static PrintWriter logWriter;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private static String currentFileName;

    /**
     * Configura el sistema de logs, creando la estructura de directorios
     * necesaria y el fichero de salida con el nombre formateado.
     *
     * @param mode Modo de ejecución ("CONCURRENT" o "SEQUENTIAL").
     * @param nAviones Número de aviones en la simulación.
     * @param nPistas Número de pistas.
     * @param nPuertas Número de puertas.
     * @param nOperarios Número de operarios (solo relevante en concurrente).
     */
    public static void setup(String mode, int nAviones, int nPistas, int nPuertas, int nOperarios) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // --- 1. Determinar subcarpeta según el modo ---
        // Si el modo contiene "SEQUENTIAL", va a logs/secuencial, si no a logs/concurrent
        String subFolder = mode.toUpperCase().contains("SEQUENTIAL") ? "secuencial" : "concurrent";
        String folderPath = "logs/" + subFolder;

        // --- 2. Crear estructura de directorios ---
        File directory = new File(folderPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs(); // Crea carpetas padre si faltan
            if (created) {
                System.out.println("--> Estructura de directorios creada: " + folderPath);
            }
        }

        // --- 3. Generar nombre de fichero según especificación ---
        // Formato: logs/carpeta/aeron-MODO-N1AV-N2PIS-N3PUE[-N4OPE]-TIMESTAMP.log
        String fileName;
        if (mode.equalsIgnoreCase("CONCURRENT")) {
            fileName = String.format("%s/aeron-%s-%dAV-%dPIS-%dPUE-%dOPE-%s.log",
                    folderPath, mode.toUpperCase(), nAviones, nPistas, nPuertas, nOperarios, timeStamp);
        } else {
            // En secuencial no solemos poner operarios en el nombre, o ponemos 1 por defecto
            fileName = String.format("%s/aeron-%s-%dAV-%dPIS-%dPUE-%s.log",
                    folderPath, mode.toUpperCase(), nAviones, nPistas, nPuertas, timeStamp);
        }

        currentFileName = fileName;

        try {
            logWriter = new PrintWriter(new FileWriter(fileName), true); // Auto-flush true
            printHeader(mode, nAviones, nPistas, nPuertas, nOperarios);
            System.out.println("--> Log de texto activo en: " + fileName);
        } catch (IOException e) {
            // Gestión de la excepción personalizada requerida en P4
            try {
                throw new LogException(fileName);
            } catch (LogException ex) {
                System.err.println("ERROR CRÍTICO LOGGER: " + ex.getMessage());
            }
        }
    }

    /**
     * Escribe la cabecera inicial del fichero de log.
     */
    private static void printHeader(String mode, int av, int pi, int pu, int op) {
        logWriter.println("====================================================================");
        logWriter.println("   AERON AIRPORT SIMULATOR - BITÁCORA DE VUELO");
        logWriter.println("====================================================================");
        logWriter.println(" Fecha inicio: " + new Date());
        logWriter.println(" Modo:         " + mode);
        logWriter.println(" Configuración: " + av + " Aviones | " + pi + " Pistas | " + pu + " Puertas");
        if (mode.equalsIgnoreCase("CONCURRENT")) {
            logWriter.println(" Operarios:    " + op);
        }
        logWriter.println("====================================================================\n");
    }

    /**
     * Registra un evento en el log de forma thread-safe.
     *
     * @param source Identificador del origen (ej. "TORRE", "IBE-001").
     * @param message Mensaje descriptivo del evento.
     */
    public static synchronized void log(String source, String message) {
        if (logWriter != null) {
            String timestamp = dateFormat.format(new Date());
            // Formato alineado: [HH:mm:ss.SSS] [ORIGEN      ] Mensaje
            logWriter.printf("[%s] [%-12s] %s%n", timestamp, source, message);

            // Verificación de errores de escritura (Requisito P4)
            if (logWriter.checkError()) {
                System.err.println(new LogException(currentFileName).getMessage());
            }
        }
    }

    /**
     * Cierra el flujo de escritura del log y finaliza el archivo.
     */
    public static void close() {
        if (logWriter != null) {
            logWriter.println("\n====================================================================");
            logWriter.println("   FIN DE LA SIMULACIÓN");
            logWriter.println("====================================================================");
            logWriter.close();
        }
    }
}

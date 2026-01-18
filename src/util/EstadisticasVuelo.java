package util;

import exceptions.CSVException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestor de estadísticas finales en formato CSV.
 * <p>
 * Registra el tiempo total de operación de cada avión y su orden de llegada.
 * </p>
 */
public class EstadisticasVuelo {

    private static PrintWriter csvWriter;
    private static final AtomicInteger ordenLlegada = new AtomicInteger(0);
    private static String currentFileName;

    /**
     * Inicializa el fichero CSV.
     */
    public static void setup() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        currentFileName = "estadisticas_" + timeStamp + ".csv";

        try {
            csvWriter = new PrintWriter(new FileWriter(currentFileName));
            csvWriter.println("Avión,Tiempo total (ms),Observaciones");
            System.out.println("--> CSV de estadísticas generado: " + currentFileName);
        } catch (IOException e) {
            // Excepción Error escritura CSV
            System.err.println(new CSVException(currentFileName).getMessage());
        }
    }

    /**
     * Registra una entrada en el CSV de forma thread-safe.
     *
     * @param avionId ID del avión.
     * @param tiempoTotal Tiempo en milisegundos desde solicitud hasta despegue.
     */
    public static synchronized void registrarVuelo(String avionId, long tiempoTotal) {
        if (csvWriter != null) {
            int posicion = ordenLlegada.incrementAndGet();
            String observacion = posicion + "º";
            csvWriter.printf("%s,%d,%s%n", avionId, tiempoTotal, observacion);

            // Verificación de errores de escritura
            if (csvWriter.checkError()) {
                System.err.println(new CSVException(currentFileName).getMessage());
            }
        }
    }

    /**
     * Cierra el fichero CSV.
     */
    public static void close() {
        if (csvWriter != null) {
            csvWriter.close();
        }
    }
}

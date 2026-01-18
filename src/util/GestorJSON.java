package util;

import exceptions.PanelException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Gestor del Panel de Vuelos JSON.
 * <p>
 * Implementa el patrón clásico <b>Lectores-Escritores</b> para garantizar la
 * consistencia de los datos al ser accedidos por múltiples hilos
 * (aviones/torre).
 * </p>
 */
public class GestorJSON {

    // TreeMap por el orden alfabetico de los aviones
    private static final Map<String, String> estadosAviones = new TreeMap<>();

    // ReadWriteLock para concurrencia eficiente
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final String FILE_NAME = "estado_vuelos.json";

    /**
     * Actualiza el estado de un avión y persiste los cambios en disco.
     * Operación de ESCRITURA (Exclusiva).
     *
     * @param idAvion ID del avión.
     * @param nuevoEstado Nuevo estado (ej. "LANDING", "DEPARTED").
     */
    public static void actualizarEstado(String idAvion, String nuevoEstado) {
        lock.writeLock().lock();
        try {
            estadosAviones.put(idAvion, nuevoEstado);
            escribirJSON();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Escribe el contenido del mapa en el fichero JSON.
     */
    private static void escribirJSON() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("{");
            int size = estadosAviones.size();
            int i = 0;
            for (Map.Entry<String, String> entry : estadosAviones.entrySet()) {
                writer.print("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
                if (++i < size) {
                    writer.println(",");
                } else {
                    writer.println("");
                }
            }
            writer.println("}");
        } catch (FileNotFoundException e) {
            // Excepción Lectura panel incorrecta
            System.err.println(new PanelException().getMessage());
        } catch (IOException e) {
            System.err.println("Error IO crítico en Panel de Vuelos: " + e.getMessage());
        }
    }
}

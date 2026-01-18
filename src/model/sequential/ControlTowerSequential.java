package model.sequential;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import util.EstadisticasVuelo;
import util.Gate;
import util.GestorJSON;
import util.Runway;
import util.Ventana;

/**
 * Torre de Control que implementa lógica SECUENCIAL.
 * <p>
 * Procesa las peticiones una a una en un único hilo (Single Threaded Executor
 * pattern). No utiliza semáforos, sino una lógica FIFO estricta.
 * </p>
 */
public class ControlTowerSequential {

    private final List<Runway> runways = new ArrayList<>();
    private final List<Gate> gates = new ArrayList<>();
    private final Queue<Request> requestQueue = new LinkedList<>();
    private final Ventana ventana;

    public ControlTowerSequential(int nRunways, int nGates, Ventana ventana) {
        this.ventana = ventana;
        for (int i = 1; i <= nRunways; i++) {
            runways.add(new Runway("P" + i));
        }
        for (int i = 1; i <= nGates; i++) {
            gates.add(new Gate("G" + i));
        }

        //Monohilo
        new Thread(this::processLoop, "Torre-Secuencial").start();
    }

    /**
     * Añade petición y actualiza el panel JSON inmediatamente.
     */
    public synchronized void addRequest(Request req) {
        // Actualización JSON
        GestorJSON.actualizarEstado(req.plane.getPlaneId(), req.type.toString());
        requestQueue.add(req);
        notifyAll();
    }

    /**
     * Bucle de procesamiento infinito.
     */
    private void processLoop() {
        while (true) {
            Request req = null;
            // Extraer
            synchronized (this) {
                while (requestQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                req = requestQueue.poll();
            }

            //Procesar 
            try {
                Thread.sleep(50);
            } catch (Exception e) {
            }

            process(req);
        }
    }

    /**
     * Lógica secuencial. 
     */
    private void process(Request req) {
        ventana.log("Secuencial: Procesando " + req.type + " de " + req.plane.getPlaneId());

        switch (req.type) {
            case LANDING:
                // Asignamos siempre los primeros recursos disponibles 
                // (asumimos disponibilidad al ser 1 a 1 en este modo simplificado)
                req.plane.assignResources(runways.get(0), gates.get(0));
                break;

            case LANDED:
                // Liberar lógica (simbólico en secuencial)
                req.plane.proceed();
                break;

            case BOARDING:
                req.plane.proceed();
                break;

            case BOARDED:
                req.plane.proceed();
                break;

            case TAKEOFF:
                req.plane.assignRunway(runways.get(0));
                break;

            case DEPARTED:
                ventana.log(req.plane.getPlaneId() + " FIN DE OPERACIÓN.");
                // Registro CSV con tiempo 0 o calculado si se desea
                EstadisticasVuelo.registrarVuelo(req.plane.getPlaneId(), 0);
                break;
        }
        ventana.updateResources();
    }

    public List<Runway> getRunways() {
        return runways;
    }

    public List<Gate> getGates() {
        return gates;
    }

    public enum RequestType {
        LANDING, LANDED, BOARDING, BOARDED, TAKEOFF, DEPARTED
    }

    public static class Request {

        RequestType type;
        PlaneSequential plane;

        public Request(RequestType t, PlaneSequential p) {
            type = t;
            plane = p;
        }
    }
}

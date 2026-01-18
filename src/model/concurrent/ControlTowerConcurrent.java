package model.concurrent;

import exceptions.ResourceException;
import exceptions.SaturationException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import util.Gate;
import util.Runway;
import util.Ventana;

/**
 * Torre de Control que implementa lógica CONCURRENTE.
 * <p>
 * Gestiona el acceso a los recursos compartidos utilizando:
 * <ul>
 * <li><b>Semáforos:</b> Para limitar el acceso a las pistas.</li>
 * <li><b>Monitores:</b> Para la exclusión mutua en la asignación de
 * puertas.</li>
 * <li><b>Colas de Espera:</b> Para evitar la pérdida de peticiones y prevenir
 * interbloqueos.</li>
 * </ul>
 * </p>
 */
public class ControlTowerConcurrent {

    private final List<Runway> runways;
    private final List<Gate> gates;

    // Semáforo para controlar el número de pistas libres
    private final Semaphore semRunways;

    // Colas de espera internas
    private final Queue<Request> waitingForLanding = new LinkedList<>();
    private final Queue<Request> waitingForTakeoff = new LinkedList<>();

    // Cola principal de peticiones (Productor-Consumidor)
    private final Queue<Request> requestQueue = new LinkedList<>();
    // Límite artificial para simular la excepción de saturación
    private static final int MAX_QUEUE_CAPACITY = 10;

    private final Object queueLock = new Object();
    private final Object logicLock = new Object();
    private final Ventana ventana;

    /**
     * Inicializa la torre concurrente.
     */
    public ControlTowerConcurrent(int nRunways, int nGates, Ventana ventana) {
        this.ventana = ventana;
        this.runways = new ArrayList<>();
        this.gates = new ArrayList<>();

        for (int i = 1; i <= nRunways; i++) {
            runways.add(new Runway("P" + i));
        }
        for (int i = 1; i <= nGates; i++) {
            gates.add(new Gate("G" + i));
        }

        // Semáforo con política FIFO 
        this.semRunways = new Semaphore(nRunways, true);
    }

    /**
     * Inicia los hilos operarios (Consumidores).
     *
     * @param nOperators Número de operarios a lanzar.
     */
    public void startOperators(int nOperators) {
        for (int i = 0; i < nOperators; i++) {
            Thread op = new Thread(new Operator(i + 1), "OP-" + i);
            op.setDaemon(true); // Daemon para que mueran al acabar el main
            op.start();
        }
    }

    /**
     * Añade una petición a la cola (Método Productor). Controla la saturación
     * de la cola lanzando excepción si está llena.
     */
    public void addRequest(Request request) {
        boolean added = false;
        while (!added) {
            try {
                synchronized (queueLock) {
                    if (requestQueue.size() >= MAX_QUEUE_CAPACITY) {
                        throw new SaturationException(request.type.toString(), request.plane.getPlaneId());
                    }
                    requestQueue.add(request);
                    queueLock.notifyAll(); // Despierta a los operarios
                    added = true;
                }
            } catch (SaturationException e) {
                System.err.println(e.getMessage());
                // Pequeña espera para no saturar CPU en reintento 
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                }
            }
        }
        ventana.log("Recibida petición: " + request);
        ventana.updateResources();
    }

    /**
     * Hilo interno que representa a un Operario de la torre.
     */
    private class Operator implements Runnable {

        private int id;

        public Operator(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Request req;
                    // Extracción segura de la cola
                    synchronized (queueLock) {
                        while (requestQueue.isEmpty()) {
                            queueLock.wait();
                        }
                        req = requestQueue.poll();
                    }

                    // Procesamiento exclusivo de lógica de recursos
                    synchronized (logicLock) {
                        processRequest(req);
                    }

                    Thread.sleep(300); // Simulación de tiempo de gestión
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Procesa la petición asignando o liberando recursos. Implementa la
     * prevención del problema de los Filósofos.
     */
    private void processRequest(Request req) {
        switch (req.type) {
            case LANDING:
                //Obtener Puerta y Pista .
                Gate freeGate = getFreeGate();

                //evita bloqueo si no hay pistas
                if (freeGate != null && semRunways.tryAcquire()) {
                    Runway r = getFreeRunwayObject();
                    r.setAvailable(false);
                    freeGate.setOccupied(true);
                    ventana.log("AUTORIZADO ATERRIZAJE: " + req.plane.getPlaneId());
                    req.plane.assignResources(r, freeGate);
                } else {
                    //excepción y  espera
                    System.err.println(new ResourceException("pista/puerta", req.plane.getPlaneId()).getMessage());
                    ventana.log("ESPERA ATERRIZAJE: " + req.plane.getPlaneId());
                    waitingForLanding.add(req);
                }
                break;

            case TAKEOFF:
                // Solo necesita pista nueva
                if (semRunways.tryAcquire()) {
                    Runway r = getFreeRunwayObject();
                    r.setAvailable(false);
                    ventana.log("AUTORIZADO DESPEGUE: " + req.plane.getPlaneId());
                    req.plane.assignRunwayForTakeoff(r);
                } else {
                    System.err.println(new ResourceException("pista", req.plane.getPlaneId()).getMessage());
                    ventana.log("ESPERA DESPEGUE: " + req.plane.getPlaneId());
                    waitingForTakeoff.add(req);
                }
                break;

            case LANDED:
                // Libera pista, mantiene puerta
                req.plane.getCurrentRunway().setAvailable(true);
                semRunways.release();
                ventana.log("Pista liberada por " + req.plane.getPlaneId());
                req.plane.proceed();
                checkPendingRequests(); // Revisa si alguien puede entrar
                break;

            case BOARDED:
                // Libera puerta
                req.plane.getCurrentGate().setOccupied(false);
                ventana.log("Puerta liberada por " + req.plane.getPlaneId());
                req.plane.proceed();
                checkPendingRequests();
                break;

            case DEPARTED:
                // Libera pista final
                req.plane.getCurrentRunway().setAvailable(true);
                semRunways.release();
                ventana.log(req.plane.getPlaneId() + " DEPARTED.");
                checkPendingRequests();
                break;

            default:
                req.plane.proceed();
                break;
        }
        ventana.updateResources();
    }

    /**
     * Revisa las colas de espera para reactivar aviones pausados cuando se
     * liberan recursos.
     */
    private void checkPendingRequests() {
        // 1: Aterrizajes
        if (!waitingForLanding.isEmpty()) {
            Gate g = getFreeGate();
            if (g != null && semRunways.tryAcquire()) {
                Request pending = waitingForLanding.poll();
                Runway r = getFreeRunwayObject();
                r.setAvailable(false);
                g.setOccupied(true);
                ventana.log("RESUMING ATERRIZAJE: " + pending.plane.getPlaneId());
                pending.plane.assignResources(r, g);
            }
        }

        //2: Despegues
        if (!waitingForTakeoff.isEmpty()) {
            if (semRunways.tryAcquire()) {
                Request pending = waitingForTakeoff.poll();
                Runway r = getFreeRunwayObject();
                r.setAvailable(false);
                ventana.log("RESUMING DESPEGUE: " + pending.plane.getPlaneId());
                pending.plane.assignRunwayForTakeoff(r);
            }
        }
    }

    private Gate getFreeGate() {
        for (Gate g : gates) {
            if (!g.isOccupied()) {
                return g;
            }
        }
        return null;
    }

    private Runway getFreeRunwayObject() {
        for (Runway r : runways) {
            if (r.isAvailable()) {
                return r;
            }
        }
        return null;
    }

    public List<Runway> getRunways() {
        return runways;
    }

    public List<Gate> getGates() {
        return gates;
    }

    public List<Request> getQueueSnapshot() {
        synchronized (queueLock) {
            return new ArrayList<>(requestQueue);
        }
    }

    /**
     * Clase interna para encapsular peticiones.
     */
    public static class Request {

        public enum Type {
            LANDING, LANDED, BOARDING, BOARDED, TAKEOFF, DEPARTED
        }
        public final Type type;
        public final Plane plane;

        public Request(Type type, Plane plane) {
            this.type = type;
            this.plane = plane;
        }

        @Override
        public String toString() {
            return type + " [" + plane.getPlaneId() + "]";
        }
    }
}

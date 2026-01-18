package model.concurrent;

import model.Passenger;
import util.EstadisticasVuelo;
import util.Gate;
import util.GestorJSON;
import util.Runway;
import util.Ventana;

/**
 * Representa un avión que opera en modo CONCURRENTE.
 * <p>
 * Este hilo gestiona su propio ciclo de vida: Vuelo, Aterrizaje ,
 * Puerta,Despegue, Salida. Se coordina con la {@link ControlTowerConcurrent}
 * mediante paso de mensajes y monitores.
 * </p>
 */
public class Plane extends Thread {

    private final String id;
    private final ControlTowerConcurrent tower;
    private final Ventana ventana;
    private final Passenger passenger; // un pasajero por avión

    // Recursos asignados por la torre
    private Runway currentRunway;
    private Gate currentGate;

    // Monitor para esperar respuesta de la torre
    private boolean waiting = true;

    /**
     * Constructor del avión concurrente.
     *
     * * @param id Identificador del avión (IBE-xxx).
     * @param tower Referencia a la torre de control concurrente.
     * @param ventana Referencia a la GUI para logs.
     */
    public Plane(String id, ControlTowerConcurrent tower, Ventana ventana) {
        this.id = id;
        this.tower = tower;
        this.ventana = ventana;
        this.passenger = new Passenger("PAX-" + id);
        // Estado inicial en el panel
        GestorJSON.actualizarEstado(id, "IN_FLIGHT");
    }

    public String getPlaneId() {
        return id;
    }

    public Runway getCurrentRunway() {
        return currentRunway;
    }

    public Gate getCurrentGate() {
        return currentGate;
    }

    /**
     * Lógica principal del ciclo de vida del avión.
     */
    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();

            //SOLICITUD DE ATERRIZAJE
            updatePhase("LANDING_REQUEST", "Solicitando pista y puerta...");
            tower.addRequest(new ControlTowerConcurrent.Request(ControlTowerConcurrent.Request.Type.LANDING, this));
            waitForTower(); // Bloqueo hasta asignación

            //ATERRIZANDO
            updatePhase("LANDING", "Aterrizando en " + currentRunway + "...");
            Thread.sleep(100); // Simulación aterrizaje

            //ATERRIZADO 
            updatePhase("LANDED", "Aterrizado. Liberando pista y yendo a " + currentGate);
            tower.addRequest(new ControlTowerConcurrent.Request(ControlTowerConcurrent.Request.Type.LANDED, this));
            waitForTower();

            //EMBARQUE
            updatePhase("BOARDING", "En puerta " + currentGate + ". Subiendo " + passenger);
            tower.addRequest(new ControlTowerConcurrent.Request(ControlTowerConcurrent.Request.Type.BOARDING, this));
            waitForTower();

            Thread.sleep(300); // Simulación embarque

            //FIN EMBARQUE 
            updatePhase("BOARDED", "Embarque finalizado. Solicitando salida.");
            tower.addRequest(new ControlTowerConcurrent.Request(ControlTowerConcurrent.Request.Type.BOARDED, this));
            waitForTower();

            // SOLICITUD DE DESPEGUE 
            updatePhase("TAKEOFF_REQUESTED", "Solicitando pista para despegue...");
            tower.addRequest(new ControlTowerConcurrent.Request(ControlTowerConcurrent.Request.Type.TAKEOFF, this));
            waitForTower();

            //DESPEGANDO
            updatePhase("DEPARTING", "Despegando por " + currentRunway + "...");
            Thread.sleep(100); // Simulación despegue

            //FINAL 
            GestorJSON.actualizarEstado(id, "DEPARTED");
            ventana.logAvion(id + ": DEPARTED. Vuelo finalizado.");
            tower.addRequest(new ControlTowerConcurrent.Request(ControlTowerConcurrent.Request.Type.DEPARTED, this));

            //Estadísticas
            long totalTime = System.currentTimeMillis() - startTime;
            EstadisticasVuelo.registrarVuelo(id, totalTime);

        } catch (InterruptedException e) {
            ventana.logAvion(id + ": Interrumpido inesperadamente.");
        }
    }

    /**
     * Actualiza el estado en el JSON y escribe un log en la ventana.
     */
    private void updatePhase(String state, String msg) {
        GestorJSON.actualizarEstado(id, state);
        ventana.logAvion(id + ": " + msg);
    }

   
    /**
     * Asigna recursos de aterrizaje y despierta al avión.
     *
     * @param r Pista asignada.
     * @param g Puerta asignada.
     */
    public synchronized void assignResources(Runway r, Gate g) {
        this.currentRunway = r;
        this.currentGate = g;
        this.waiting = false;
        notify();
    }

    /**
     * Asigna pista de despegue y despierta al avión.
     *
     * @param r Pista asignada.
     */
    public synchronized void assignRunwayForTakeoff(Runway r) {
        this.currentRunway = r;
        this.waiting = false;
        notify();
    }

    /**
     * Despierta al avión para que continúe a la siguiente fase.
     */
    public synchronized void proceed() {
        this.waiting = false;
        notify();
    }

    /**
     * Espera pasiva hasta ser notificado por la torre.
     */
    private synchronized void waitForTower() throws InterruptedException {
        while (waiting) {
            wait();
        }
        waiting = true; // Reset para la próxima espera
    }

    @Override
    public String toString() {
        return id;
    }
}

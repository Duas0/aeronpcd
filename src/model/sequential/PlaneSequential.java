package model.sequential;

import model.Passenger;
import util.Gate;
import util.Runway;
import util.Ventana;

/**
 * Representa un avión que opera en modo SECUENCIAL.
 */
public class PlaneSequential extends Thread {

    private final String id;
    private final ControlTowerSequential tower;
    private final Ventana ventana;
    private final Passenger passenger;

    private Runway currentRunway;
    private Gate currentGate;
    private boolean waiting = true;

    public PlaneSequential(String id, ControlTowerSequential tower, Ventana ventana) {
        this.id = id;
        this.tower = tower;
        this.ventana = ventana;
        this.passenger = new Passenger("PAX-" + id);
    }

    public String getPlaneId() {
        return id;
    }

    @Override
    public void run() {
        try {
            // Ciclo de vida secuencial
            //Aterrizaje
            ventana.logAvion(id + ": LANDING_REQUEST");
            tower.addRequest(new ControlTowerSequential.Request(ControlTowerSequential.RequestType.LANDING, this));
            waitForTower(); // Espera asignación

            Thread.sleep(100); // Aterrizando

            //Liberar pista
            tower.addRequest(new ControlTowerSequential.Request(ControlTowerSequential.RequestType.LANDED, this));
            waitForTower();

            // Embarque
            tower.addRequest(new ControlTowerSequential.Request(ControlTowerSequential.RequestType.BOARDING, this));
            waitForTower();
            ventana.logAvion(id + ": Subiendo " + passenger);
            Thread.sleep(300);

            //Fin Embarque
            tower.addRequest(new ControlTowerSequential.Request(ControlTowerSequential.RequestType.BOARDED, this));
            waitForTower();

            // Despegue
            tower.addRequest(new ControlTowerSequential.Request(ControlTowerSequential.RequestType.TAKEOFF, this));
            waitForTower(); // Espera asignación pista nueva

            Thread.sleep(100); // Despegando

            //Fin
            tower.addRequest(new ControlTowerSequential.Request(ControlTowerSequential.RequestType.DEPARTED, this));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // sincronización 
    public synchronized void assignResources(Runway r, Gate g) {
        this.currentRunway = r;
        this.currentGate = g;
        this.waiting = false;
        notify();
    }

    public synchronized void assignRunway(Runway r) {
        this.currentRunway = r;
        this.waiting = false;
        notify();
    }

    public synchronized void proceed() {
        this.waiting = false;
        notify();
    }

    private synchronized void waitForTower() throws InterruptedException {
        while (waiting) {
            wait();
        }
        waiting = true;
    }

    @Override
    public String toString() {
        return id;
    }
}

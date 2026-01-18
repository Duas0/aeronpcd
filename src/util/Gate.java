package util;

/**
 * Representa una puerta de embarque.
 * <p>
 * Recurso compartido donde los aviones estacionan para la carga de pasajeros.
 * </p>
 */
public class Gate {

    private final String id;
    private boolean occupied = false;

    /**
     * @param id Identificador de la puerta
     */
    public Gate(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID de puerta inválido");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Comprueba si la puerta está ocupada de forma sincronizada.
     */
    public synchronized boolean isOccupied() {
        return occupied;
    }

    /**
     * Ocupa o libera la puerta de forma sincronizada.
     */
    public synchronized void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    @Override
    public String toString() {
        return "Puerta " + id;
    }
}

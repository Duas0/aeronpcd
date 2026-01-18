package util;

/**
 * Representa una pista de aterrizaje o despegue.
 * <p>
 * Recurso compartido que solo puede ser utilizado por un avión a la vez.
 * </p>
 */
public class Runway {

    private final String id;
    private boolean available = true;

    /**
     * @param id Identificador de la pista (ej. "P1").
     */
    public Runway(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID de pista inválido");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Comprueba si la pista está libre de forma sincronizada.
     */
    public synchronized boolean isAvailable() {
        return available;
    }

    /**
     * Ocupa o libera la pista de forma sincronizada.
     */
    public synchronized void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "Pista " + id;
    }
}

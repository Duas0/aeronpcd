package model;

/**
 * Representa a un pasajero de un avión.
 * <p>
 * Clase simple utilizada para simular la carga de pasaje durante la fase de
 * embarque.
 * </p>
 */
public class Passenger {

    private final String id;

    /**
     * Crea un nuevo pasajero.
     *
     * @param id Identificador del pasajero (PAX-IBE-xxx).
     */
    public Passenger(String id) {
        this.id = id;
    }

    /**
     * Obtiene el identificador del pasajero.
     *
     * @return ID del pasajero.
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Pasajero " + id;
    }
}

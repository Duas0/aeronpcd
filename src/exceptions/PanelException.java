package exceptions;

/**
 * Excepción lanzada cuando falla la actualización o lectura del Panel de
 * Vuelos.
 */
public class PanelException extends AeronException {

    public PanelException() {
        super("No se ha actualizado el panel de vuelos. Fichero JSON no encontrado");
    }
}

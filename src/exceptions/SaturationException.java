package exceptions;

/**
 * Excepción lanzada cuando la cola de peticiones de la torre está saturada.
 * Indica que la petición debe ser reintentada más tarde.
 */
public class SaturationException extends AeronException {

    public SaturationException(String tipoPeticion, String idAvion) {
        super("Cola de peticiones completa, reintentando más tarde la captura de la petición " + tipoPeticion + " del avión " + idAvion);
    }
}

package exceptions;

/**
 * Excepción lanzada cuando no es posible asignar un recurso solicitado (Pista o
 * Puerta).
 */
public class ResourceException extends AeronException {

    public ResourceException(String recurso, String idAvion) {
        super("No se ha podido asignar " + recurso + " al avión " + idAvion);
    }
}

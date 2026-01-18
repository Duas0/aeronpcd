package exceptions;

/**
 * Excepción base para el sistema AERON. Permite capturar cualquier error
 * específico del dominio de forma genérica.
 */
public class AeronException extends Exception {

    public AeronException(String message) {
        super(message);
    }
}

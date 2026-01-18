package exceptions;

/**
 * Excepción lanzada cuando la configuración inicial de la simulación es
 * inválida. (Ej: número de aviones negativo, más pistas que puertas, etc.)
 */
public class AeronConfigException extends AeronException {

    public AeronConfigException(String message) {
        super("ERROR DE CONFIGURACIÓN: " + message);
    }
}

package exceptions;

/**
 * Excepción lanzada cuando no se puede crear o escribir en el fichero de log.
 */
public class LogException extends AeronException {

    public LogException(String fileName) {
        super("No se ha encontrado el archivo de log " + fileName);
    }
}

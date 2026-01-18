package exceptions;

/**
 * Excepción lanzada cuando falla la escritura del fichero de estadísticas CSV.
 */
public class CSVException extends AeronException {

    public CSVException(String fileName) {
        super("Error al escribir el resumen de la simulación. No se ha podido guardar en el fichero " + fileName);
    }
}

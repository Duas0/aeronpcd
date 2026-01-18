package util;

import model.concurrent.ControlTowerConcurrent;
import model.sequential.ControlTowerSequential;
import javax.swing.*;
import java.awt.*;

/**
 * Interfaz Gráfica de Usuario (GUI) para la simulación AERON.
 * <p>
 * Muestra tres paneles principales: 1. Eventos de los aviones. 2. Eventos de la
 * torre de control. 3. Estado visual de los recursos (Pistas/Puertas) y la cola
 * de peticiones.
 * </p>
 * <p>
 * Es capaz de visualizar tanto el modo Secuencial como el Concurrente.
 * </p>
 */
public class Ventana extends JFrame {

    private JTextArea areaAviones;
    private JTextArea areaTorre;
    private JTextArea areaRecursos;

    // Referencias a los posibles tipos de torre (Polimorfismo ad-hoc)
    private ControlTowerConcurrent towerConcurrent;
    private ControlTowerSequential towerSequential;

    /**
     * Constructor de la ventana principal. Configura el Layout y componentes
     * Swing.
     */
    public Ventana() {
        super("AERON Simulator - Panel de Control");
        setLayout(new GridLayout(1, 3));

        areaAviones = createArea("Bitácora de Aviones");
        areaTorre = createArea("Operaciones Torre de Control");
        areaRecursos = createArea("Estado de Recursos / Cola");

        // Fuente monoespaciada obligatoria para el arte ASCII de AirportState
        areaRecursos.setFont(new Font("Monospaced", Font.PLAIN, 12));

        add(new JScrollPane(areaAviones));
        add(new JScrollPane(areaTorre));
        add(new JScrollPane(areaRecursos));

        setSize(1400, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private JTextArea createArea(String title) {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setBorder(BorderFactory.createTitledBorder(title));
        return ta;
    }

    /**
     * Asigna la torre de control a la ventana. Detecta dinámicamente si es
     * secuencial o concurrente.
     *
     * @param tower Instancia de la torre (ControlTowerConcurrent o
     * ControlTowerSequential).
     */
    public void setTower(Object tower) {
        if (tower instanceof ControlTowerConcurrent) {
            this.towerConcurrent = (ControlTowerConcurrent) tower;
            this.towerSequential = null;
        } else if (tower instanceof ControlTowerSequential) {
            this.towerSequential = (ControlTowerSequential) tower;
            this.towerConcurrent = null;
        }
    }

    /**
     * Registra un mensaje en el panel de aviones y en el log de disco.
     *
     * @param msg Mensaje del avión.
     */
    public void logAvion(String msg) {
        SwingUtilities.invokeLater(() -> {
            areaAviones.append(msg + "\n");
            // Auto-scroll al final
            areaAviones.setCaretPosition(areaAviones.getDocument().getLength());
        });
        // Extraemos el ID del avión para el log estructurado (formato ID: Mensaje)
        String id = msg.contains(":") ? msg.split(":")[0] : "AVION";
        SimulationLogger.log(id, msg);
    }

    /**
     * Registra un mensaje en el panel de la torre y en el log de disco.
     *
     * @param msg Mensaje de la torre.
     */
    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            areaTorre.append(msg + "\n");
            areaTorre.setCaretPosition(areaTorre.getDocument().getLength());
        });
        SimulationLogger.log("TORRE", msg);
    }

    /**
     * Actualiza el panel derecho con el estado visual de los recursos. Utiliza
     * {@link AirportState} para formatear el texto.
     */
    public void updateResources() {
        SwingUtilities.invokeLater(() -> {
            String status = "";
            String queue = "";

            if (towerConcurrent != null) {
                // Modo Concurrente: Mostramos pistas, puertas y la cola real
                status = AirportState.showResourcesStatus(towerConcurrent.getRunways(), towerConcurrent.getGates());
                queue = AirportState.showRequestQueue(towerConcurrent.getQueueSnapshot());
            } else if (towerSequential != null) {
                // Modo Secuencial: Mostramos pistas y puertas
                status = AirportState.showResourcesStatus(towerSequential.getRunways(), towerSequential.getGates());
                queue = "\n[MODO SECUENCIAL]\nProcesamiento FIFO estricto.\nCola interna gestionada secuencialmente.";
            }

            areaRecursos.setText(status + "\n" + queue);
        });
    }
}

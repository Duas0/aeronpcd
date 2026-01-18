package util;

import java.util.ArrayList;
import java.util.List;
import model.concurrent.ControlTowerConcurrent.Request;

/**
 * Clase de utilidad para formatear visualmente el estado del aeropuerto. Genera
 * representaciones en texto para la interfaz gráfica.
 */
public class AirportState {

    /**
     * Formatea la cola de peticiones de la torre concurrente.
     */
    public static String showRequestQueue(List<Request> requestQueue) {
        if (requestQueue == null) {
            return "Cola de peticiones: vacía";
        }

        int n = requestQueue.size();
        List<String> lines = new ArrayList<>();
        String title = "Cola de peticiones (" + n + ")";

        if (n == 0) {
            return createBoxedString(title + ": vacía");
        }

        lines.add(title + ":");
        int i = 1;
        for (Request r : requestQueue) {
            String tipo = (r.type != null) ? r.type.toString() : "UNKNOWN";
            // 1) TIPO — AVION
            lines.add(String.format("  %d) %-17s — %s", i++, tipo, r.plane));
        }

        return createBoxedStringFromLines(lines);
    }

    /**
     * Formatea el estado de Pistas y Puertas.
     */
    public static String showResourcesStatus(List<Runway> runways, List<Gate> gates) {
        List<String> lines = new ArrayList<>();
        lines.add(" ESTADO DE RECURSOS ");
        lines.add("Pistas:");
        if (runways.isEmpty()) {
            lines.add(" (Sin datos)");
        } else {
            StringBuilder sbId = new StringBuilder("  ");
            StringBuilder sbSt = new StringBuilder("  ");
            for (Runway r : runways) {
                String icon = r.isAvailable() ? "🟢" : "🔴";
                int w = Math.max(r.getId().length(), 2) + 3;
                sbId.append(String.format("%-" + w + "s", r.getId()));
                sbSt.append(String.format("%-" + w + "s", icon));
            }
            lines.add(sbId.toString());
            lines.add(sbSt.toString());
        }

        lines.add("");
        lines.add("Puertas:");
        if (gates.isEmpty()) {
            lines.add(" (Sin datos)");
        } else {
            StringBuilder sbId = new StringBuilder("  ");
            StringBuilder sbSt = new StringBuilder("  ");
            for (Gate g : gates) {
                String icon = g.isOccupied() ? "🔴" : "🟢";
                int w = Math.max(g.getId().length(), 2) + 3;
                sbId.append(String.format("%-" + w + "s", g.getId()));
                sbSt.append(String.format("%-" + w + "s", icon));
            }
            lines.add(sbId.toString());
            lines.add(sbSt.toString());
        }

        return createBoxedStringFromLines(lines);
    }

    //ASCII 
    private static String createBoxedString(String content) {
        int width = content.length();
        String border = repeat('═', width);
        return "╔" + border + "╗\n║" + content + "║\n╚" + border + "╝";
    }

    private static String createBoxedStringFromLines(List<String> lines) {
        int max = 0;
        for (String l : lines) {
            if (l.length() > max) {
                max = l.length();
            }
        }

        String border = repeat('═', max);
        StringBuilder sb = new StringBuilder();

        sb.append("\n╔").append(border).append("╗\n");
        for (String l : lines) {
            sb.append("║");
            sb.append(l);
            int padding = max - l.length();
            if (padding > 0) {
                sb.append(repeat(' ', padding));
            }
            sb.append("║\n");
        }
        sb.append("╚").append(border).append("╝");
        return sb.toString();
    }

    private static String repeat(char ch, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}

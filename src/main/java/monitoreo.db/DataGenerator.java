package monitoreo.db;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Random;
import java.util.Scanner;

public class DataGenerator {

    private static final Random random = new Random();

    public static void main(String[] args) {

        System.out.println("--- Generador de Datos Históricos ---");

        try (Scanner scanner = new Scanner(System.in)) {

            // --- 1. Solicitar la Fecha ---
            LocalDate fechaAInsertar = solicitarFecha(scanner);

            if (fechaAInsertar != null) {
                // --- 2. Generar y Persistir los Datos ---
                generarDatosParaDia(fechaAInsertar);
            } else {
                System.out.println("Operación cancelada o formato de fecha incorrecto.");
            }

        } catch (Exception e) {
            System.err.println("Ocurrió un error inesperado en la ejecución: " + e.getMessage());
        }

        System.out.println("--- Inserción de datos históricos finalizada. ---");
    }

    /**
     * Solicita al usuario la fecha de inserción y valida el formato dd-mm-aaaa.
     */
    private static LocalDate solicitarFecha(Scanner scanner) {
        System.out.println("Por favor, introduce la fecha del registro (formato: DD-MM-AAAA):");
        String fechaStr = scanner.nextLine().trim();

        // El formato que el usuario debe seguir
        DateTimeFormatter userInputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try {
            // Intentar parsear la fecha ingresada
            return LocalDate.parse(fechaStr, userInputFormatter);

        } catch (DateTimeParseException e) {
            System.err.println("Error: Formato de fecha invlido. Asegúrate de usar DD-MM-AAAA.");
            return null;
        }
    }

    /**
     * Genera 100 registros para la fecha especificada.
     */
    private static void generarDatosParaDia(LocalDate fecha) {

        // Formatos de salida para la DB (YYYY-MM-DD y HH:MM:SS)
        DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        String fechaStr = fecha.format(fechaFormatter);

        System.out.println("Generando 100 registros para el día: " + fechaStr + "...");

        int insercionesExitosas = 0;

        for (int i = 0; i < 100; i++) {

            // Simular valores de acelerómetro (rango -1024 a 1024)
            int x = random.nextInt(2048) - 1024;
            int y = random.nextInt(2048) - 1024;
            int z = random.nextInt(2048) - 1024;

            // Simular diferentes horas dentro del día
            LocalDateTime timestamp = fecha.atStartOfDay()
                    .plusHours(random.nextInt(24))
                    .plusMinutes(random.nextInt(60))
                    .plusSeconds(random.nextInt(60));

            String fechaGuardar = timestamp.format(fechaFormatter);
            String horaGuardar = timestamp.format(horaFormatter);

            boolean exito = ConexionBD.guardarDatos(x, y, z, fechaGuardar, horaGuardar);

            if (exito) {
                insercionesExitosas++;
            }
        }
        System.out.println("✅ " + insercionesExitosas + " registros insertados para el da " + fechaStr);
    }
}
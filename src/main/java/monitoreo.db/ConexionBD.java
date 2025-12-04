package monitoreo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.ResultSet; // ¡Nuevo Import!
import java.util.ArrayList; // ¡Nuevo Import!
import java.util.List;

public class ConexionBD {

    // Nombre del archivo de la base de datos (se creará en la raíz del proyecto)
    private static final String URL = "jdbc:sqlite:monitorBD.db";

    /**
     * Obtiene y retorna una conexión a la base de datos SQLite.
     * @return Objeto Connection o null si hay un error.
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Asegura que el driver JDBC de SQLite esté cargado
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(URL);
            // System.out.println("Conexión a la base de datos establecida.");
            return conn;
        } catch (SQLException e) {
            System.err.println("Error de SQL al conectar: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC de SQLite no encontrado.");
        }
        return null;
    }

    /**
     * Crea la tabla 'datos_sensor' si no existe.
     */
    public static void crearTabla() {
        String sql = """
            CREATE TABLE IF NOT EXISTS datos_sensor (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                fecha_de_captura TEXT NOT NULL,
                hora_de_captura TEXT NOT NULL
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            if (conn != null) {
                // Ejecutar el query de creación de la tabla
                stmt.execute(sql);
                System.out.println("Tabla 'datos_sensor' verificada/creada correctamente.");
            }

        } catch (SQLException e) {
            System.err.println("Error al crear la tabla: " + e.getMessage());
        }
    }

    /**
     * Cierra la conexión a la base de datos.
     * @param conn La conexión a cerrar.
     */
    public static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
    /**
     * Inserta un nuevo registro de datos del sensor en la tabla 'datos_sensor'.
     * @param x Valor del eje X.
     * @param y Valor del eje Y.
     * @param z Valor del eje Z.
     * @return true si la inserción fue exitosa, false en caso contrario.
     */
    public static boolean insertarDatos(int x, int y, int z) {
        String sql = "INSERT INTO datos_sensor (x, y, z, fecha_de_captura, hora_de_captura) VALUES (?, ?, ?, ?, ?)";

        // 1. Obtener la fecha y hora actual en el formato de la BD
        LocalDateTime now = LocalDateTime.now();
        String fecha = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String hora = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        // Usamos try-with-resources para asegurar el cierre de Connection y PreparedStatement
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("No se pudo obtener la conexión para la inserción.");
                return false;
            }

            // 2. Asignar los valores a los placeholders (?)
            pstmt.setInt(1, x);
            pstmt.setInt(2, y);
            pstmt.setInt(3, z);
            pstmt.setString(4, fecha);
            pstmt.setString(5, hora);

            // 3. Ejecutar la inserción
            int affectedRows = pstmt.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar datos: " + e.getMessage());
            return false;
        }
    }
    /**
     * Consulta los datos de la tabla 'datos_sensor' aplicando filtros opcionales.
     * @param fechaInicio Filtro de fecha de inicio (formato yyyy-MM-dd), null para ignorar.
     * @param horaInicio Filtro de hora de inicio (formato HH:mm:ss), null para ignorar.
     * @return Una lista de Strings, donde cada String es una fila de datos (ej: "x,y,z,fecha,hora").
     */
    public static List<String> consultarDatos(String fechaInicio, String horaInicio) {

        List<String> registros = new ArrayList<>();
        String sql = "SELECT x, y, z, fecha_de_captura, hora_de_captura FROM datos_sensor";

        // Lógica de filtrado simple. El cliente solo solicita fecha y hora, no un rango.
        // Si ambos filtros están presentes, asumimos que es un filtro de punto de inicio
        // para traer todo lo posterior.
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            // Esta lógica es simplificada para filtrar desde una fecha/hora dada
            sql += " WHERE fecha_de_captura >= ? AND hora_de_captura >= ?";
        }

        sql += " ORDER BY fecha_de_captura, hora_de_captura ASC"; // Ordenar cronológicamente

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) return registros;

            // Asignar parámetros si se usan filtros
            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                pstmt.setString(1, fechaInicio);
                pstmt.setString(2, horaInicio);
            }

            // 1. Ejecutar la consulta
            ResultSet rs = pstmt.executeQuery();

            // 2. Procesar el resultado
            while (rs.next()) {
                String fila = rs.getInt("x") + ","
                        + rs.getInt("y") + ","
                        + rs.getInt("z") + ","
                        + rs.getString("fecha_de_captura") + ","
                        + rs.getString("hora_de_captura");
                registros.add(fila);
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar datos: " + e.getMessage());
        }
        return registros;
    }
}

package monitoreo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConexionBD {

    private static final String URL = "jdbc:sqlite:monitorBD.db";

    /**
     * Obtiene y retorna una conexión a la base de datos SQLite.
     * @return Objeto Connection o null si hay un error.
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(URL);
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
     * NOTA: La tabla usa nombres de columna cortos (x, y, z).
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

    // --- MÉTODO 1: INSERCIÓN EN TIEMPO REAL (Usa la hora actual) ---

    public static boolean insertarDatos(int x, int y, int z) {
        LocalDateTime now = LocalDateTime.now();
        String fecha = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String hora = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        return guardarDatos(x, y, z, fecha, hora);
    }

    // --- MÉTODO 2: GUARDAR DATOS CON FECHA ESPECÍFICA (CORREGIDO) ---

    /**
     * Guarda datos del sensor en la DB usando una fecha y hora proporcionadas.
     * @param x Valor del eje X.
     * @param y Valor del eje Y.
     * @param z Valor del eje Z.
     * @param fechaStr Fecha (yyyy-MM-dd).
     * @param horaStr Hora (HH:mm:ss).
     * @return true si la inserción fue exitosa.
     */
    public static boolean guardarDatos(int x, int y, int z, String fechaStr, String horaStr) {
        // 🚨 CORRECCIÓN: Usar x, y, z en lugar de eje_x, eje_y, eje_z
        String sql = "INSERT INTO datos_sensor (x, y, z, fecha_de_captura, hora_de_captura) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) {
                System.err.println("No se pudo obtener la conexión para la inserción.");
                return false;
            }

            // Asignar los valores
            pstmt.setInt(1, x);
            pstmt.setInt(2, y);
            pstmt.setInt(3, z);
            pstmt.setString(4, fechaStr);
            pstmt.setString(5, horaStr);

            // Ejecutar la inserción
            int affectedRows = pstmt.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar datos: " + e.getMessage());
            return false;
        }
    }

    // --- MÉTODO 3: CONSULTA DE DATOS HISTÓRICOS (CORREGIDO) ---

    /**
     * Consulta los datos de la tabla 'datos_sensor' aplicando filtro por día.
     * @param filtroFecha Filtro de fecha (formato yyyy-MM-dd). Si es nulo, trae todos.
     * @return Una lista de Strings, donde cada String es una fila de datos.
     */
    public static List<String> consultarDatos(String filtroFecha) {

        List<String> registros = new ArrayList<>();
        // 🚨 CORRECCIÓN: Usar x, y, z en lugar de eje_x, eje_y, eje_z
        String sql = "SELECT id, x, y, z, fecha_de_captura, hora_de_captura FROM datos_sensor";

        // Lógica de Filtrado: Filtramos por el campo 'fecha_de_captura'
        if (filtroFecha != null && !filtroFecha.isEmpty()) {
            sql += " WHERE fecha_de_captura = ?";
        }

        // Ordenar cronológicamente
        sql += " ORDER BY fecha_de_captura, hora_de_captura ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) return registros;

            // Asignar parámetros si se usan filtros
            if (filtroFecha != null && !filtroFecha.isEmpty()) {
                pstmt.setString(1, filtroFecha);
            }

            // Ejecutar la consulta
            ResultSet rs = pstmt.executeQuery();

            // Procesar el resultado
            while (rs.next()) {
                // Formato de salida requerido por el Cliente: ID,X,Y,Z,Fecha,Hora
                String fila = rs.getInt("id") + ","
                        + rs.getInt("x") + "," // 🚨 CORRECCIÓN: Leer la columna 'x'
                        + rs.getInt("y") + "," // 🚨 CORRECCIÓN: Leer la columna 'y'
                        + rs.getInt("z") + "," // 🚨 CORRECCIÓN: Leer la columna 'z'
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
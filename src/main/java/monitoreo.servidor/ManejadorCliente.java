package monitoreo.servidor;

import monitoreo.db.ConexionBD;
import monitoreo.util.CifradoUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List; // Necesario para la lista de resultados
import java.util.ArrayList; // Necesario para la lista de resultados

// Clase que maneja la comunicación con UN cliente específico en su propio Thread
public class ManejadorCliente implements Runnable {

    private final Socket clienteSocket;

    public ManejadorCliente(Socket socket) {
        this.clienteSocket = socket;
        System.out.println("1. Nuevo cliente conectado desde: " + socket.getInetAddress().getHostAddress());
    }

    @Override
    public void run() {
        try (
                // Streams para enviar y recibir datos
                PrintWriter out = new PrintWriter(clienteSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()))
        ) {
            String inputLine;
            // Leer datos del cliente línea por línea
            while ((inputLine = in.readLine()) != null) {
                // El servidor espera recibir un mensaje encriptado
                procesarPeticion(inputLine, out);
            }

        } catch (IOException e) {
            // Esto ocurre cuando el cliente cierra la conexión (se desconecta)
            System.err.println("Error de I/O en la comunicación con el cliente: " + e.getMessage());
        } finally {
            try {
                clienteSocket.close();
                System.out.println("Cliente desconectado: " + clienteSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }

    /**
     * Analiza el mensaje recibido (encriptado), lo desencripta y realiza la acción.
     */
    private void procesarPeticion(String mensajeEncriptado, PrintWriter out) {

        // 1. DESENCRIPTAR EL MENSAJE
        String mensajeDesencriptado = CifradoUtil.decrypt(mensajeEncriptado);

        if (mensajeDesencriptado == null) {
            System.err.println("Error: No se pudo desencriptar el mensaje. Descartando.");
            return;
        }

        // 2. Identificar el tipo de petición
        if (mensajeDesencriptado.startsWith("GUARDAR:")) {
            String datos = mensajeDesencriptado.substring("GUARDAR:".length());
            System.out.println("2. Cliente solicitó guardar datos: " + datos);

            String respuesta;
            boolean guardadoExitoso = manejarGuardado(datos);

            if (guardadoExitoso) {
                respuesta = "ACK:Datos guardados exitosamente.";
                System.out.println("4. Se envió confirmación de guardado.");
            } else {
                respuesta = "ERROR:Fallo al guardar los datos en la base de datos.";
                System.err.println("4. Fallo al guardar los datos.");
            }

            // 3. Enviar respuesta encriptada
            String respuestaEncriptada = CifradoUtil.encrypt(respuesta);
            out.println(respuestaEncriptada);

        } else if (mensajeDesencriptado.startsWith("CONSULTAR:")) {
            // Ejemplo de mensaje esperado: "CONSULTAR:2025-12-01" (solo fecha)
            String filtros = mensajeDesencriptado.substring("CONSULTAR:".length());
            System.out.println("3. Cliente solicitó consulta histórica con filtros: " + filtros);

            // 1. Asumimos que el filtro es la fecha (YYYY-MM-DD)
            String fechaFiltro = filtros.trim().isEmpty() ? null : filtros.trim();

            // 2. Consultar la base de datos
            // El método consultaDatos ahora espera solo la fecha de filtro.
            List<String> datos = ConexionBD.consultarDatos(fechaFiltro);

            // 3. Formatear y enviar los datos
            // 🚨 CORRECCIÓN: Usar la variable 'datos' en lugar de la variable no declarada 'registros'
            String datosParaEnviar = String.join("|", datos);

            // Si no hay datos, enviamos un mensaje de error o vacío
            String datosEncriptados;
            if (datos.isEmpty()) {
                datosEncriptados = CifradoUtil.encrypt("ERROR:No se encontraron datos con esos filtros.");
                System.out.println("4. Se envió ERROR de consulta.");
            } else {
                // El formato final enviado será: "DATA:ID,x,y,z,f,h|ID,x,y,z,f,h|..."
                String mensajeFinal = "DATA:" + datosParaEnviar;
                datosEncriptados = CifradoUtil.encrypt(mensajeFinal);
                System.out.println("4. Se enviaron " + datos.size() + " registros.");
            }

            out.println(datosEncriptados);

        } else {
            System.err.println("Petición no reconocida: " + mensajeDesencriptado);
        }
    }

    /**
     * Parsea la cadena de datos (x,y,z) y los inserta en la BD.
     */
    private boolean manejarGuardado(String datos) {
        try {
            // Esperamos un formato "X,Y,Z"
            String[] partes = datos.split(",");
            if (partes.length != 3) {
                System.err.println("Formato de datos incorrecto: Se esperaban X,Y,Z.");
                return false;
            }

            // Parsear y limpiar espacios
            int x = Integer.parseInt(partes[0].trim());
            int y = Integer.parseInt(partes[1].trim());
            int z = Integer.parseInt(partes[2].trim());

            // Llamada al método de inserción en la BD (usa fecha y hora actuales)
            boolean exito = ConexionBD.insertarDatos(x, y, z);

            if (exito) {
                System.out.println("3. Datos insertados en la DB: X=" + x + ", Y=" + y + ", Z=" + z);
            }
            return exito;

        } catch (NumberFormatException e) {
            System.err.println("Error de formato: X, Y, Z deben ser números enteros. Mensaje: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error inesperado al guardar datos: " + e.getMessage());
            return false;
        }
    }
}
package monitoreo.servidor;

import monitoreo.db.ConexionBD;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorApp {

    public static final int PUERTO_BASE = 5000;

    public static void main(String[] args) {

        System.out.println("Iniciando Servidor...");
        ConexionBD.crearTabla(); // Asegura la existencia de la DB y la tabla

        try (ServerSocket serverSocket = new ServerSocket(PUERTO_BASE)) {

            System.out.println("Servidor iniciado y esperando conexiones en el puerto: " + PUERTO_BASE);

            // Loop principal: Escuchar indefinidamente nuevas conexiones de clientes
            while (true) {
                // Bloquea hasta que un cliente se conecta
                Socket clienteSocket = serverSocket.accept();

                // Crea un nuevo hilo para manejar la comunicación con el cliente
                Thread t = new Thread(new ManejadorCliente(clienteSocket));
                t.start();
            }

        } catch (IOException e) {
            System.err.println("Error de I/O en el servidor: " + e.getMessage());
            // Si el puerto está ocupado u otro error grave
        }
    }
}
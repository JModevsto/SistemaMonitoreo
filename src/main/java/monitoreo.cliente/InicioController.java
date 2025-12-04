package monitoreo.cliente;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.io.File;
import java.io.IOException;

public class InicioController {

    // Componentes de la interfaz gráfica (fx:id)
    @FXML private Button btnMonitor;
    @FXML private Button btnHistorico;
    @FXML private Button btnServidor;

    private Process servidorProcess; // Referencia al proceso del servidor

    @FXML
    public void initialize() {
        // Método llamado después de que el FXML carga
        // Esto solo es una verificación inicial, el proceso se actualiza en iniciarServidor
        if (servidorProcess != null && servidorProcess.isAlive()) {
            btnServidor.setDisable(true); // Deshabilita si ya está corriendo
        }
    }

    // --- MÉTODOS DE NAVEGACIÓN ---

    @FXML
    private void irAMonitor() throws IOException {
        System.out.println("Navegando a Monitor...");
        // 🚨 CAMBIO: Cargar la vista del monitor
        ClienteApp.loadView("monitorview.fxml");
    }

    @FXML
    private void irAHistorico() throws IOException {
        System.out.println("Navegando a Histórico...");
        // 🚨 CAMBIO: Cargar la vista de histórico
        // NOTA: Asumo que nombrarás este FXML como "historicoview.fxml"
        ClienteApp.loadView("historicoview.fxml");
    }

    // --- LÓGICA DE INICIO DE SERVIDOR ---

    /**
     * Inicia el programa Servidor como un proceso separado, asegurando que el classpath
     * incluya los drivers de SQLite (sqlite-jdbc) y SLF4J (slf4j-api) necesarios.
     */
    @FXML
    private void iniciarServidor() {
        if (servidorProcess != null && servidorProcess.isAlive()) {
            System.out.println("Servidor ya está corriendo.");
            return;
        }

        try {
            // 1. Definir rutas necesarias
            String projectPath = new File("").getAbsolutePath();
            String targetClasses = projectPath + File.separator + "target" + File.separator + "classes";
            String homePath = System.getProperty("user.home");

            // 2. Rutas a los JARs de dependencias externas que debe tener el Servidor
            String sqlitePath = homePath +
                    File.separator + ".m2" + File.separator + "repository" + File.separator + "org" +
                    File.separator + "xerial" + File.separator + "sqlite-jdbc" + File.separator + "3.45.1.0" +
                    File.separator + "sqlite-jdbc-3.45.1.0.jar";

            String slf4jApiPath = homePath +
                    File.separator + ".m2" + File.separator + "repository" + File.separator + "org" +
                    File.separator + "slf4j" + File.separator + "slf4j-api" + File.separator + "1.7.36" +
                    File.separator + "slf4j-api-1.7.36.jar";

            // 3. Construir el Classpath Completo (Separado por File.pathSeparator, que es ';')
            String fullClasspath = targetClasses +
                    File.pathSeparator + sqlitePath +
                    File.pathSeparator + slf4jApiPath;

            // 4. Configurar ProcessBuilder para lanzar el comando 'java'
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp",
                    fullClasspath, // Usamos el Classpath completo
                    "monitoreo.servidor.ServidorApp" // Clase main a ejecutar
            );

            pb.directory(new File(projectPath));
            pb.inheritIO(); // Redirige la salida del Servidor a la consola del Cliente

            servidorProcess = pb.start();

            if (servidorProcess.isAlive()) {
                System.out.println("✅ Servidor iniciado correctamente.");
                btnServidor.setDisable(true);
            } else {
                System.err.println("❌ Error al iniciar el servidor.");
            }

        } catch (IOException e) {
            System.err.println("Excepción al ejecutar el servidor: " + e.getMessage());
        }
    }
}
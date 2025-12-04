package monitoreo.cliente;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox; // Dejamos el ComboBox para que el FXML no falle

import java.io.IOException;
import java.util.Random;

public class MonitorController {

    // Componentes de la interfaz gráfica
    @FXML private ComboBox<String> cbPuertos;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private Button btnIniciarDetener;
    @FXML private Label lblX, lblY, lblZ; // Para mostrar valores de la última lectura

    private SocketCliente clienteSocket = new SocketCliente();
    private boolean lecturaActiva = false;
    private Thread threadGeneradorDatos; // ¡Ahora genera, no lee!

    // Series para la gráfica
    private XYChart.Series<Number, Number> seriesX, seriesY, seriesZ;
    private int tiempo = 0;

    // Rango de simulación para el acelerómetro
    private static final int MIN_VAL = -1024;
    private static final int MAX_VAL = 1024;
    private Random random = new Random();

    @FXML
    public void initialize() {
        inicializarGrafica();
        // El ComboBox ya no se usa para Serial, pero lo mantenemos visible.
        cbPuertos.setVisible(false);
        btnIniciarDetener.setText("Iniciar Monitoreo");
        btnIniciarDetener.setDisable(false);
    }

    // --- INICIALIZACIÓN DE GUI ---

    private void inicializarGrafica() {
        seriesX = new XYChart.Series<>();
        seriesY = new XYChart.Series<>();
        seriesZ = new XYChart.Series<>();

        seriesX.setName("Eje X");
        seriesY.setName("Eje Y");
        seriesZ.setName("Eje Z");

        lineChart.getData().addAll(seriesX, seriesY, seriesZ);
        lineChart.setAnimated(false);
    }

    // El método cargarPuertos() y seleccionarPuerto() ya no son necesarios
    // pero mantenemos el ComboBox visible para evitar errores de FXML.

    // --- EVENTOS Y CONTROL ---

    @FXML
    private void seleccionarPuerto() { /* No hace nada */ }

    @FXML
    private void iniciarDetenerLectura() {
        if (!lecturaActiva) {
            iniciarMonitoreo();
        } else {
            detenerMonitoreo();
        }
    }

    @FXML
    private void irAtras() throws IOException {
        if (lecturaActiva) detenerMonitoreo();
        // Asumimos que ClienteApp tiene el loadView para regresar al inicio
        ClienteApp.loadView("inicioview.fxml");
    }

    private void iniciarMonitoreo() {
        // 1. Conectar Socket (Asegurarse que el Servidor esté corriendo)
        if (!clienteSocket.conectar()) {
            System.err.println("Error: El servidor no está activo. Conecte el servidor primero.");
            return;
        }

        System.out.println("Monitoreo SIMULADO iniciado.");
        lecturaActiva = true;
        btnIniciarDetener.setText("Detener Monitoreo");

        // 2. Iniciar el hilo generador (Worker Thread)
        threadGeneradorDatos = new Thread(this::generarYProcesarDatos);
        threadGeneradorDatos.setDaemon(true);
        threadGeneradorDatos.start();
    }

    private void detenerMonitoreo() {
        lecturaActiva = false;
        if (threadGeneradorDatos != null) threadGeneradorDatos.interrupt();
        clienteSocket.desconectar();

        btnIniciarDetener.setText("Iniciar Monitoreo");
    }

    // --- LÓGICA DE SIMULACIÓN Y PROCESAMIENTO ---

    private void generarYProcesarDatos() {
        while (lecturaActiva) {
            try {
                // 1. Generar valores aleatorios que simulan la lectura del acelerómetro
                int x = random.nextInt(MAX_VAL - MIN_VAL) + MIN_VAL;
                int y = random.nextInt(MAX_VAL - MIN_VAL) + MIN_VAL;
                int z = random.nextInt(MAX_VAL - MIN_VAL) + MIN_VAL;

                // 2. Ejecutar la actualización de GUI y envío en el hilo de JavaFX
                Platform.runLater(() -> actualizarGraficaYEnviar(x, y, z));

                // Esperar un breve período (simula la velocidad de lectura del puerto serial)
                Thread.sleep(100);
            } catch (InterruptedException e) {
                lecturaActiva = false;
            }
        }
    }

    private void actualizarGraficaYEnviar(int x, int y, int z) {
        // 1. Actualizar Labels de última lectura
        lblX.setText(String.valueOf(x));
        lblY.setText(String.valueOf(y));
        lblZ.setText(String.valueOf(z));

        // 2. Añadir el punto a la gráfica (JavaFX Thread)
        tiempo++;
        seriesX.getData().add(new XYChart.Data<>(tiempo, x));
        seriesY.getData().add(new XYChart.Data<>(tiempo, y));
        seriesZ.getData().add(new XYChart.Data<>(tiempo, z));

        // Control de puntos
        if (seriesX.getData().size() > 50) {
            seriesX.getData().remove(0);
            seriesY.getData().remove(0);
            seriesZ.getData().remove(0);
        }

        // 3. Enviar los datos al servidor
        enviarDatosAlServidor(x, y, z);
    }

    private void enviarDatosAlServidor(int x, int y, int z) {
        // Esto previene que el envío de red bloquee el hilo de JavaFX
        new Thread(() -> {
            boolean exito = clienteSocket.enviarDatosMonitoreo(x, y, z);
            if (!exito) {
                Platform.runLater(() -> {
                    System.err.println("Lectura detenida: Fallo en la comunicación de red.");
                    detenerMonitoreo();
                });
            }
        }).start();
    }
}
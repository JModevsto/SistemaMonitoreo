package monitoreo.cliente;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HistoricoController {

    // Componentes del filtro y gráfico (IDs del FXML)
    @FXML private DatePicker dpFechaFiltro;
    @FXML private LineChart<Number, Number> lineChartHistorico;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Series para la gráfica
    private XYChart.Series<Number, Number> seriesX, seriesY, seriesZ;

    private SocketCliente clienteSocket = new SocketCliente();

    @FXML
    public void initialize() {
        // Inicializar las series de la gráfica
        seriesX = new XYChart.Series<>();
        seriesY = new XYChart.Series<>();
        seriesZ = new XYChart.Series<>();

        seriesX.setName("Eje X");
        seriesY.setName("Eje Y");
        seriesZ.setName("Eje Z");

        lineChartHistorico.getData().addAll(seriesX, seriesY, seriesZ);
        lineChartHistorico.setAnimated(false);

        // Inicializar con la fecha de hoy para el filtro
        dpFechaFiltro.setValue(LocalDate.now());
    }

    @FXML
    private void cargarDatosHistoricos() {
        // 1. Obtener la fecha seleccionada del filtro
        LocalDate selectedDate = dpFechaFiltro.getValue();
        if (selectedDate == null) {
            System.err.println("Seleccione una fecha para filtrar.");
            return;
        }

        // Formatear la fecha a YYYY-MM-DD
        String filterDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Ejecutamos la carga en un hilo de fondo
        new Thread(() -> {
            if (!clienteSocket.conectar()) {
                Platform.runLater(() -> System.err.println("Error: El servidor no está activo para la consulta."));
                return;
            }

            // 2. Solicitar datos usando la fecha como filtro
            String datosRaw = clienteSocket.solicitarDatosHistoricos(filterDate);
            clienteSocket.desconectar();

            // Actualizar la GUI (limpiar y título) en el hilo de JavaFX
            Platform.runLater(() -> {
                seriesX.getData().clear();
                seriesY.getData().clear();
                seriesZ.getData().clear();
                lineChartHistorico.setTitle("Registro Histórico del Día: " + filterDate);
            });

            if (datosRaw != null && !datosRaw.isEmpty()) {
                String[] registros = datosRaw.split("\\|");
                int timeIndex = 0;

                for (String registro : registros) {
                    if (registro.isEmpty()) continue;

                    // El formato esperado es X, Y, Z, FechaCompleta (sin el ID al inicio)
                    String[] campos = registro.split(",");

                    // Nos aseguramos de que haya al menos 3 campos (X, Y, Z)
                    if (campos.length >= 3) {
                        try {
                            // 🚨 CORRECCIÓN: Usar índices 0, 1, 2 para X, Y, Z
                            int x = Integer.parseInt(campos[0].trim());
                            int y = Integer.parseInt(campos[1].trim());
                            int z = Integer.parseInt(campos[2].trim());

                            timeIndex++;
                            final int finalTimeIndex = timeIndex;

                            // 4. Agregar puntos a las series en el hilo de JavaFX
                            Platform.runLater(() -> {
                                seriesX.getData().add(new XYChart.Data<>(finalTimeIndex, x));
                                seriesY.getData().add(new XYChart.Data<>(finalTimeIndex, y));
                                seriesZ.getData().add(new XYChart.Data<>(finalTimeIndex, z));
                            });

                        } catch (NumberFormatException e) {
                            // Ignora los registros que no se puedan convertir a números
                            System.err.println("Error al parsear valores numéricos (registro ignorado): " + registro);
                        }
                    } else {
                        System.err.println("Registro ignorado por falta de campos: " + registro);
                    }
                }
                final int finalTotalSamples = timeIndex;
                Platform.runLater(() -> System.out.println("✅ Datos históricos graficados. Total de muestras: " + finalTotalSamples));
            } else {
                Platform.runLater(() -> System.out.println("No se encontraron datos para la fecha: " + filterDate));
            }
        }).start();
    }

    @FXML
    private void irAtras() throws IOException {
        ClienteApp.loadView("inicioview.fxml");
    }
}
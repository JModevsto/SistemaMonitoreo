package monitoreo.cliente;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClienteApp extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("Sistema de Monitoreo - Cliente");

        // Carga la primera vista
        loadView("inicioview.fxml");
    }

    /**
     * Método estático para cambiar el contenido de la ventana (navegación).
     * @param fxmlName El nombre del archivo FXML (ej. "inicioview.fxml").
     */
    public static void loadView(String fxmlName) throws IOException {

        // CONSTRUCCIÓN DE LA RUTA ABSOLUTA PARA RECURSOS:
        // /paquete/nombre_archivo.fxml
        String resourcePath = "/monitoreo/cliente/" + fxmlName;

        // Intentar cargar el recurso
        // Usamos ClientApp.class para buscar el recurso desde el mismo ClassLoader
        FXMLLoader fxmlLoader = new FXMLLoader(ClienteApp.class.getResource(resourcePath));

        // Verifica que el recurso haya sido encontrado (evita el error Location is not set)
        if (fxmlLoader.getLocation() == null) {
            System.err.println("❌ ERROR FATAL: No se pudo encontrar el recurso FXML: " + resourcePath);
            throw new IOException("Recurso FXML no encontrado: " + resourcePath + ". Verifique su ubicación en src/main/resources/monitoreo/cliente/");
        }

        Parent root = fxmlLoader.load();

        Scene scene = primaryStage.getScene();
        if (scene == null) {
            // Define el tamaño inicial
            scene = new Scene(root, 1000, 650);
            primaryStage.setScene(scene);
            primaryStage.show();
        } else {
            // Cambia la raíz de la escena existente
            scene.setRoot(root);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
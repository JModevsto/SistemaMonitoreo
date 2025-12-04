package monitoreo.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class CifradoUtil {

    // --- Clave Secreta ---
    // NOTA IMPORTANTE: Esta clave debe ser IGUAL en el Cliente y en el Servidor.
    // Usaremos un String base para generar una clave AES de 128 bits.
    private static final String CLAVE_SECRETA_BASE = "MonitoreoUnison2025";

    private static SecretKeySpec secretKey;

    // Bloque estático para inicializar la clave secreta una sola vez al cargar la clase
    static {
        try {
            byte[] keyBytes = CLAVE_SECRETA_BASE.getBytes(StandardCharsets.UTF_8);
            // Tomamos los primeros 16 bytes (128 bits) para AES
            keyBytes = java.util.Arrays.copyOf(keyBytes, 16);
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            System.err.println("Error al inicializar la clave de cifrado: " + e.getMessage());
        }
    }

    /**
     * Encripta una cadena de texto usando AES.
     * @param strToEncrypt El string a encriptar.
     * @return El string encriptado codificado en Base64.
     */
    public static String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            // El resultado binario se codifica a Base64 para enviarlo como String
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.err.println("Error al encriptar: " + e.toString());
            return null;
        }
    }

    /**
     * Desencripta una cadena de texto codificada en Base64 usando AES.
     * @param strToDecrypt El string encriptado codificado en Base64.
     * @return El string desencriptado original.
     */
    public static String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            // Se decodifica de Base64 antes de desencriptar
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Error al desencriptar. Clave o datos incorrectos: " + e.toString());
            return null;
        }
    }
}
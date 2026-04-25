package net.ljcomputing.camelpoc.util;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Command-line utility that derives a Base64-encoded HS256 key from a plain text
 * password and writes it to the jwt.secret property in application.yml.
 *
 * The password is stretched via PBKDF2WithHmacSHA256 (310,000 iterations, 256-bit output)
 * using a random salt. The derived key — not the password — is stored.
 *
 * Run from the project root after compiling:
 *   java -cp build/classes/java/main net.ljcomputing.camelpoc.util.JwtSecretUpdater
 */
public class JwtSecretUpdater {

    private static final Logger LOG = Logger.getLogger(JwtSecretUpdater.class.getName());

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITERATIONS = 310_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    static final Path CONFIG_PATH =
        Paths.get("src", "main", "resources", "application.yml");

    public static void main(String[] args) throws IOException {
        Console console = System.console();
        if (console == null) {
            LOG.severe("No interactive console available. Run this outside of an IDE piped environment.");
            System.exit(1);
        } else {
            char[] password = console.readPassword("Enter password (input hidden): ");
            if (password == null || password.length == 0) {
                LOG.severe("No password entered. Aborting.");
                System.exit(1);
            }

            try {
                updateSecret(CONFIG_PATH, deriveBase64Key(password));
                LOG.log(Level.INFO, "jwt.secret updated in {0}", CONFIG_PATH);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOG.severe(() -> "Key derivation failed: " + e.getMessage());
                System.exit(1);
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }

    static String deriveBase64Key(char[] password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, KDF_ITERATIONS, KEY_LENGTH_BITS);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance(KDF_ALGORITHM)
                .generateSecret(spec)
                .getEncoded();
            return Base64.getEncoder().encodeToString(keyBytes);
        } finally {
            spec.clearPassword();
        }
    }

    static void updateSecret(Path configPath, String secret) throws IOException {
        List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
        List<String> result = new ArrayList<>(lines.size());

        boolean inJwtBlock = false;
        boolean updated = false;

        for (String line : lines) {
            String trimmed = line.stripLeading();

            // Detect top-level block boundaries (non-empty, non-comment lines with no leading whitespace)
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !Character.isWhitespace(line.charAt(0))) {
                inJwtBlock = line.stripTrailing().equals("jwt:");
            }

            if (inJwtBlock && trimmed.startsWith("secret:")) {
                String indent = line.substring(0, line.indexOf("secret:"));
                result.add(indent + "secret: " + secret);
                updated = true;
            } else {
                result.add(line);
            }
        }

        if (!updated) {
            throw new IllegalStateException("Could not find jwt.secret in " + configPath);
        }

        Files.write(configPath, result);
    }
}

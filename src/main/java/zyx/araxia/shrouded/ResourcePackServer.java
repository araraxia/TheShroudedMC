package zyx.araxia.shrouded;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hosts the plugin resource pack over a minimal built-in HTTP server so the
 * pack can be served without external infrastructure.
 *
 * <h3>Setup</h3>
 * <ol>
 * <li>Zip the {@code resourcepack/} directory (with {@code pack.mcmeta} at
 * the root of the zip) and name it {@code resourcepack.zip}.</li>
 * <li>Drop {@code resourcepack.zip} into the plugin's data folder
 * ({@code plugins/TheShrouded/resourcepack.zip}).</li>
 * <li>Set {@code resource-pack.port} and {@code resource-pack.server-ip}
 * in {@code config.yml}.</li>
 * </ol>
 *
 * <p>
 * Call {@link #start()} in {@code onEnable} and {@link #stop()} in
 * {@code onDisable}.
 */
public class ResourcePackServer {

    private static final String PACK_FILENAME = "resourcepack.zip";

    private final File packFile;
    private final int port;
    private final Logger logger;

    private HttpServer httpServer;
    private byte[] packBytes;
    private String sha1Hex;

    public ResourcePackServer(File dataFolder, int port, Logger logger) {
        this.packFile = new File(dataFolder, PACK_FILENAME);
        this.port = port;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Reads {@code resourcepack.zip}, computes its SHA-1, and starts the HTTP
     * server. Does nothing (logs a warning) if the file is missing.
     */
    public void start() {
        if (!packFile.exists()) {
            logger.log(Level.WARNING,
                    "[TheShrouded] Resource pack server NOT started — {0} not found. "
                            + "Place a resourcepack.zip in the plugin data folder.",
                    packFile.getPath());
            return;
        }

        try {
            packBytes = readFile(packFile);
            sha1Hex = sha1(packBytes);

            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/" + PACK_FILENAME, exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, packBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(packBytes);
                }
            });
            httpServer.setExecutor(null); // uses the default executor
            httpServer.start();

            logger.log(Level.INFO,
                    "[TheShrouded] Resource pack server started on port {0} (SHA-1: {1}).",
                    new Object[] { port, sha1Hex });
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "[TheShrouded] Failed to start resource pack server.", e);
        }
    }

    /** Stops the HTTP server if it is running. */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            logger.log(Level.INFO, "[TheShrouded] Resource pack server stopped.");
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the server is running and the pack is available.
     */
    public boolean isRunning() {
        return httpServer != null;
    }

    /**
     * Returns the hex SHA-1 of the resource pack zip, or {@code null} if the
     * server has not started successfully.
     */
    public String getSha1Hex() {
        return sha1Hex;
    }

    /**
     * Builds the full URL to the resource pack zip using the given server IP.
     *
     * @param serverIp the external IP or hostname players can reach
     * @return URL string, e.g. {@code http://123.45.67.89:8080/resourcepack.zip}
     */
    public String getUrl(String serverIp) {
        return "http://" + serverIp + ":" + port + "/" + PACK_FILENAME;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    private static String sha1(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
        StringBuilder sb = new StringBuilder(40);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

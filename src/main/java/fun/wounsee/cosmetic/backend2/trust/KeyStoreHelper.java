package fun.wounsee.cosmetic.backend2.trust;

import com.neovisionaries.ws.client.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.backend2.NetworkStuff;
import fun.wounsee.cosmetic.backend2.websocket.FiguraWebSocketAdapter;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.utils.PlatformUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyStoreHelper {

    private static final char[] password = "figuramc".toCharArray();

    public static WebSocket websocketWithBackendCertificates(String token) throws WebSocketException {
        CosmeticMod.LOGGER.info("Initializing custom websocket");
        try {
            WebSocketFactory wsFactory = new WebSocketFactory();
            String serverName = ServerAddress.parseString(Configs.SERVER_IP.value).getHost();
            wsFactory.setServerName(serverName);
            WebSocket socket = wsFactory.createSocket(FiguraWebSocketAdapter.getBackendAddress());
            socket.addListener(new FiguraWebSocketAdapter(token));
            socket.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
            return socket;
        } catch (IOException e) {
            CosmeticMod.LOGGER.error("Failed to load in the backend's certificates during Websocket creation!", e);
            NetworkStuff.disconnect("Failed to load certificates for the backend :c");
        }
        throw new WebSocketException(WebSocketError.SOCKET_CONNECT_ERROR);
    }

    // Not needed on anything newer than Java 8
    // Derived from https://github.com/MinecraftForge/Installer/blob/1.x/src/main/java/net/minecraftforge/installer/FixSSL.java
    private static KeyStore getKeyStore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        final KeyStore jdkKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Path ksPath = Paths.get(System.getProperty("java.home"),"lib", "security", "cacerts");
        jdkKeyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());
        final Map<String, Certificate> jdkTrustStore = Collections.list(jdkKeyStore.aliases()).stream().collect(Collectors.toMap(a -> a, (String alias) -> {
            try {
                return jdkKeyStore.getCertificate(alias);
            } catch (KeyStoreException e) {
                CosmeticMod.LOGGER.error("Could not find default certificates!", e);
            }
            return null;
        }));


        InputStream in = PlatformUtils.loadFileFromRoot("figurakeystore.jks");
        KeyStore figuraKeyStore = KeyStore.getInstance("JKS");
        figuraKeyStore.load(in, password);
        if (in != null) {
            in.close();
        }
        final Map<String, Certificate> figuraTrustStore = Collections.list(figuraKeyStore.aliases()).stream().collect(Collectors.toMap(a -> a, (String alias) -> {
            try {
                return figuraKeyStore.getCertificate(alias);
            } catch (KeyStoreException e) {
                CosmeticMod.LOGGER.error("Could not find default certificates!", e);
            }
            return null;
        }));

        final KeyStore mergedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        mergedTrustStore.load(null, new char[0]);
        for (Map.Entry<String, Certificate> entry : jdkTrustStore.entrySet()) {
            mergedTrustStore.setCertificateEntry(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String , Certificate> entry : figuraTrustStore.entrySet()) {
            mergedTrustStore.setCertificateEntry(entry.getKey(), entry.getValue());
        }

        return mergedTrustStore;
    }
}

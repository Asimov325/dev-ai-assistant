package com.dev.aiassistant.config.service;

import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.model.IntegrationConfig;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

@Service
public class LocalConfigurationStore {
    private static final String ENC_PREFIX = "ENC:";
    private final Path root = Path.of(System.getProperty("user.home"), ".development-ai-assistant");
    private final Path keyFile = root.resolve(".config.key");
    private final SecureRandom random = new SecureRandom();

    public synchronized void saveGit(List<ConfiguredGitRepository> repositories) {
        Properties p = new Properties();
        p.setProperty("count", String.valueOf(repositories.size()));
        for (int i = 0; i < repositories.size(); i++) {
            var r = repositories.get(i);
            String x = "repo." + i + ".";
            put(p, x + "name", r.name());
            put(p, x + "source", r.source());
            put(p, x + "location", r.location());
            put(p, x + "username", r.username());
            put(p, x + "secret", encrypt(r.secret()));
            put(p, x + "branches", String.join("\u001F", r.branches() == null ? List.of() : r.branches()));
        }
        write(root.resolve("git.properties"), p);
    }

    public synchronized List<ConfiguredGitRepository> loadGit() {
        Properties p = read(root.resolve("git.properties"));
        List<ConfiguredGitRepository> result = new ArrayList<>();
        int count = parseInt(p.getProperty("count"));
        for (int i = 0; i < count; i++) {
            String x = "repo." + i + ".";
            String branches = p.getProperty(x + "branches", "");
            result.add(new ConfiguredGitRepository(p.getProperty(x + "name", ""), p.getProperty(x + "source", "LOCAL"), p.getProperty(x + "location", ""), p.getProperty(x + "username", ""), decrypt(p.getProperty(x + "secret", "")), branches.isBlank() ? List.of() : List.of(branches.split("\u001F"))));
        }
        return result;
    }

    public synchronized void saveIntegration(String name, IntegrationConfig config) {
        if (config == null) {
            delete(name);
            return;
        }
        Properties p = new Properties();
        put(p, "url", config.url());
        put(p, "username", config.username());
        put(p, "secret", encrypt(config.secret()));
        put(p, "context", config.context());
        write(root.resolve(name + ".properties"), p);
    }

    public synchronized IntegrationConfig loadIntegration(String name) {
        Properties p = read(root.resolve(name + ".properties"));
        if (p.isEmpty()) return null;
        return new IntegrationConfig(p.getProperty("url", ""), p.getProperty("username", ""), decrypt(p.getProperty("secret", "")), p.getProperty("context", ""));
    }

    public synchronized void delete(String name) {
        try {
            Files.deleteIfExists(root.resolve(name + ".properties"));
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible eliminar la configuración local.", ex);
        }
    }

    private Properties read(Path path) {
        Properties p = new Properties();
        if (!Files.exists(path)) return p;
        try (var in = Files.newInputStream(path)) {
            p.load(in);
            return p;
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible leer la configuración local.", ex);
        }
    }

    private void write(Path path, Properties p) {
        try {
            Files.createDirectories(root);
            try (var out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                p.store(out, "Development AI Assistant - configuración local. No versionar.");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible guardar la configuración local.", ex);
        }
    }

    private void put(Properties p, String k, String v) {
        p.setProperty(k, v == null ? "" : v);
    }

    private int parseInt(String v) {
        try {
            return Integer.parseInt(v == null ? "0" : v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String encrypt(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return ENC_PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible proteger la credencial local.", ex);
        }
    }

    private String decrypt(String value) {
        if (value == null || value.isBlank()) return "";
        if (!value.startsWith(ENC_PREFIX)) return value;
        try {
            String[] parts = value.substring(ENC_PREFIX.length()).split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(c.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible recuperar una credencial local protegida.", ex);
        }
    }

    private SecretKey key() throws Exception {
        Files.createDirectories(root);
        byte[] bytes;
        if (Files.exists(keyFile)) {
            bytes = Base64.getDecoder().decode(Files.readString(keyFile).trim());
        } else {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            bytes = kg.generateKey().getEncoded();
            Files.writeString(keyFile, Base64.getEncoder().encodeToString(bytes), StandardOpenOption.CREATE_NEW);
        }
        return new SecretKeySpec(bytes, "AES");
    }
}

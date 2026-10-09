package com.finora.security;

import com.finora.config.Database;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** AES-GCM field encryption for finance records. The key ring lives beside, never inside, SQLite. */
public final class FinanceCipher {
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Path keyringPath;
    private final Map<String, byte[]> keys = new LinkedHashMap<>();
    private String activeKeyId;

    public FinanceCipher(Database database) throws Exception {
        String configured = System.getenv("FINORA_KEY_PATH");
        keyringPath = configured == null || configured.isBlank()
                ? database.getDatabasePath().resolveSibling("finora.keyring")
                : Path.of(configured).toAbsolutePath();
        if (Files.exists(keyringPath)) { loadKeyring(); restrictPermissions(keyringPath); }
        else {
            if (databaseContainsCiphertext(database)) throw new IllegalStateException("Finora's encryption key ring is missing. Restore the key ring backup before starting the app.");
            Files.createDirectories(keyringPath.getParent());
            addKeyAndActivate(randomKey());
            saveKeyring();
        }
    }

    public synchronized String encryptFields(String... values) {
        try {
            ByteArrayOutputStream plainBytes = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(plainBytes)) {
                data.writeInt(values.length);
                for (String value : values) data.writeUTF(value == null ? "" : value);
            }
            byte[] nonce = new byte[NONCE_BYTES]; RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keys.get(activeKeyId), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainBytes.toByteArray());
            ByteArrayOutputStream packed = new ByteArrayOutputStream();
            packed.write(nonce); packed.write(encrypted);
            return "v1." + activeKeyId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(packed.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encrypt finance data", exception);
        }
    }

    public synchronized String[] decryptFields(String encoded, int expectedCount) {
        try {
            String[] parts = encoded.split("\\.", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw new IllegalArgumentException("Unrecognized encrypted data format");
            byte[] key = keys.get(parts[1]);
            if (key == null) throw new IllegalStateException("The key needed to read finance data is missing from the key ring");
            byte[] packed = Base64.getUrlDecoder().decode(parts[2]);
            byte[] nonce = java.util.Arrays.copyOfRange(packed, 0, NONCE_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(packed, NONCE_BYTES, packed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] plain = cipher.doFinal(encrypted);
            try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(plain))) {
                int count = data.readInt();
                if (count != expectedCount) throw new IllegalStateException("Encrypted data has an unexpected field count");
                String[] values = new String[count];
                for (int i = 0; i < count; i++) values[i] = data.readUTF();
                if (data.available() != 0) throw new IllegalStateException("Encrypted data contains trailing bytes");
                return values;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decrypt finance data. Preserve the Finora key ring and restore it if needed.", exception);
        }
    }

    public synchronized String getEncryptionLabel() { return "AES-256-GCM · key " + activeKeyId.substring(0, 8); }
    public synchronized String getActiveKeyId() { return activeKeyId; }
    public Path getKeyringPath() { return keyringPath; }

    public synchronized void activateNewKey() throws Exception {
        addKeyAndActivate(randomKey());
        saveKeyring();
    }

    private String payloadKeyId(String payload) {
        if (payload == null || !payload.startsWith("v1.")) return null;
        String[] parts = payload.split("\\.", 3);
        return parts.length > 1 ? parts[1] : null;
    }

    private void addKeyAndActivate(byte[] key) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        keys.put(id, key);
        activeKeyId = id;
    }

    private static byte[] randomKey() {
        byte[] key = new byte[32]; RANDOM.nextBytes(key); return key;
    }

    private void loadKeyring() throws Exception {
        List<String> lines = Files.readAllLines(keyringPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            int separator = line.indexOf('=');
            if (separator < 1) continue;
            String name = line.substring(0, separator);
            String value = line.substring(separator + 1);
            if ("active".equals(name)) activeKeyId = value;
            else {
                byte[] key = Base64.getDecoder().decode(value);
                if (key.length != 32) throw new IllegalStateException("Invalid key length in Finora key ring");
                keys.put(name, key);
            }
        }
        if (activeKeyId == null || !keys.containsKey(activeKeyId)) throw new IllegalStateException("Finora key ring is invalid or incomplete");
    }

    private void saveKeyring() throws Exception {
        Files.createDirectories(keyringPath.getParent());
        Path temporary = keyringPath.resolveSibling(keyringPath.getFileName() + ".tmp");
        StringBuilder contents = new StringBuilder("active=").append(activeKeyId).append('\n');
        for (Map.Entry<String, byte[]> entry : keys.entrySet()) contents.append(entry.getKey()).append('=').append(Base64.getEncoder().encodeToString(entry.getValue())).append('\n');
        Files.writeString(temporary, contents, StandardCharsets.UTF_8);
        restrictPermissions(temporary);
        try { Files.move(temporary, keyringPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, keyringPath, StandardCopyOption.REPLACE_EXISTING); }
        restrictPermissions(keyringPath);
    }

    private static void restrictPermissions(Path file) {
        try { Files.setPosixFilePermissions(file, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)); }
        catch (Exception ignored) { /* The key remains outside the web root; document platform-specific filesystem permissions. */ }
    }

    private static boolean databaseContainsCiphertext(Database database) throws Exception {
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            for (String table : List.of("expenses", "budgets", "goals", "recurring_expenses")) {
                try (var rows = statement.executeQuery("SELECT 1 FROM " + table + " WHERE encrypted_payload IS NOT NULL LIMIT 1")) {
                    if (rows.next()) return true;
                }
            }
        }
        return false;
    }

    public synchronized void reencrypt(Database database) throws Exception {
        try (var connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (String table : List.of("expenses", "budgets", "goals", "recurring_expenses")) {
                    List<Map.Entry<Long, String>> payloads = new ArrayList<>();
                    try (var query = connection.prepareStatement("SELECT id,encrypted_payload FROM " + table + " WHERE encrypted_payload IS NOT NULL"); var rs = query.executeQuery()) {
                        while (rs.next()) payloads.add(Map.entry(rs.getLong(1), rs.getString(2)));
                    }
                    for (Map.Entry<Long, String> row : payloads) {
                        String oldKey = payloadKeyId(row.getValue());
                        if (oldKey == null || oldKey.equals(activeKeyId)) continue;
                        int count = table.equals("recurring_expenses") ? 6 : 4;
                        String[] values = decryptFields(row.getValue(), count);
                        String newPayload = encryptFields(values);
                        try (var update = connection.prepareStatement("UPDATE " + table + " SET encrypted_payload=? WHERE id=?")) {
                            update.setString(1, newPayload); update.setLong(2, row.getKey()); update.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}

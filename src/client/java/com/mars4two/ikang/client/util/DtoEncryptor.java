package com.mars4two.ikang.client.util;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * Encrypts DTOs with the AFKraft RSA public key bundled as a client resource.
 *
 * <p>Produces ciphertext equivalent to:
 * <pre>
 *   openssl pkeyutl -encrypt -pubin -inkey afkraft-mvp.public.pem
 * </pre>
 * (RSA with PKCS#1 v1.5 padding), but encoded as unpadded Base64URL so the
 * result is safe to embed in a URL query parameter.
 */
public final class DtoEncryptor {

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-Encryptor");

    private static final String PUBLIC_KEY_RESOURCE = "/afkraft-mvp.public.pem";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int PKCS1_PADDING_OVERHEAD_BYTES = 11;

    private static final Gson GSON = new Gson();

    private static RSAPublicKey publicKey;

    private DtoEncryptor() {
    }

    /**
     * Serializes the given DTO to JSON, encrypts it with the RSA public key and
     * returns the ciphertext as unpadded Base64URL, safe for URL query params.
     *
     * @throws DtoEncryptionException if the key cannot be loaded, the payload
     *         exceeds the RSA block limit, or encryption fails
     */
    public static String encryptToBase64Url(Object dto) {
        LOGGER.info("Encrypting DTO: {}", dto);
        byte[] jsonBytes = GSON.toJson(dto).getBytes(StandardCharsets.UTF_8);
        RSAPublicKey key = loadPublicKey();

        int maxPayloadBytes = (key.getModulus().bitLength() + 7) / Byte.SIZE - PKCS1_PADDING_OVERHEAD_BYTES;
        if (jsonBytes.length > maxPayloadBytes) {
            throw new DtoEncryptionException(
                "DTO is too large to encrypt: " + jsonBytes.length + " bytes (max " + maxPayloadBytes + ")");
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(jsonBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            LOGGER.error("Failed to encrypt DTO", e);
            throw new DtoEncryptionException("Failed to encrypt DTO", e);
        }
    }

    private static synchronized RSAPublicKey loadPublicKey() {
        if (publicKey == null) {
            publicKey = readPublicKey();
        }
        return publicKey;
    }

    private static RSAPublicKey readPublicKey() {
        LOGGER.info("Reading RSA public key from resource: {}", PUBLIC_KEY_RESOURCE);
        String pem;
        try (InputStream in = DtoEncryptor.class.getResourceAsStream(PUBLIC_KEY_RESOURCE)) {
            if (in == null) {
                throw new DtoEncryptionException("Public key resource not found: " + PUBLIC_KEY_RESOURCE);
            }
            pem = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new DtoEncryptionException("Failed to read public key resource", e);
        }

        String base64 = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return (RSAPublicKey) KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            LOGGER.error("Failed to parse RSA public key", e);
            throw new DtoEncryptionException("Failed to parse RSA public key", e);
        }
    }

    public static class DtoEncryptionException extends RuntimeException {
        public DtoEncryptionException(String message) {
            super(message);
        }

        public DtoEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

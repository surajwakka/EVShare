package com.evshare.service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Paytm checksum implementation for the JSON-body signature format. */
public final class PaytmChecksum {
    private static final String IV = "@@@@&&&&####$$$$";
    private static final String SALT_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();
    private PaytmChecksum() {}

    public static String generate(String body, String merchantKey) {
        try {
            StringBuilder salt = new StringBuilder(4);
            for (int i=0;i<4;i++) salt.append(SALT_CHARS.charAt(RANDOM.nextInt(SALT_CHARS.length())));
            String hash = sha256(body + "|" + salt) + salt;
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(merchantKey), "AES"), new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));
            return Base64.getEncoder().encodeToString(cipher.doFinal(hash.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Unable to generate Paytm checksum", e); }
    }

    public static boolean verify(String body, String merchantKey, String signature) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(merchantKey), "AES"), new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));
            String decoded = new String(cipher.doFinal(Base64.getDecoder().decode(signature)), StandardCharsets.UTF_8);
            if (decoded.length() < 5) return false;
            String salt = decoded.substring(decoded.length()-4);
            return MessageDigest.isEqual(decoded.getBytes(StandardCharsets.UTF_8), (sha256(body + "|" + salt) + salt).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { return false; }
    }

    private static byte[] key(String merchantKey) throws Exception {
        if (merchantKey == null || merchantKey.getBytes(StandardCharsets.UTF_8).length != 16)
            throw new IllegalArgumentException("Paytm AES-128 merchant key must be 16 bytes; use the key from your Paytm dashboard");
        return merchantKey.getBytes(StandardCharsets.UTF_8);
    }
    private static String sha256(String value) throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
    }
}

package com.deepshikhayadav.seat_reservation.show;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    public static String sha256(String input) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                        input.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(
                    String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
package ru.rudn.rudnadmin.service.postgres.utils;

import java.security.SecureRandom;

public class PostgresUtils {


    public static String substringEmail(final String email) {
        final String base = email.toLowerCase().substring(0, email.indexOf('@'));

        return base.replace(".", "_");
    }

    public static String generatePassword() {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_-+=";
        final SecureRandom rnd = new SecureRandom();
        final StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }

        return sb.toString();
    }

}

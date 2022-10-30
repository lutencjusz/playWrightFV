package org.example.utils;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CryptoText {

    //    private static SecretKey keyAes = null;
    private static SecretKey keyDes = null;
    private static final String pathStr = "src/test/resources/.env";
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(pathStr)
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    private static SecretKey createDESKey(String keyDesString) throws Exception {
        DESKeySpec dks = new DESKeySpec(keyDesString.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(dks);
    }

    private static void saveKey(String value) {
        Path p = Paths.get(pathStr);
        String s = System.lineSeparator() + "KEY_DES=" + value;
        System.out.println(ConsoleColors.YELLOW + "Nie znalazłem klucza " + "KEY_DES=" + ", generuje nowy i zapisuje do .env (" + pathStr + ")...");
        try {
            Files.write(p, s.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println(ConsoleColors.RED_BOLD + "Wystąpił błąd:" + ex.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void init() throws Exception {
        if (keyDes == null) {
            String keyDesString = dotenv.get("KEY_DES");
            String SOIL = dotenv.get("SOIL");
            if (keyDesString == null) {
                assert SOIL != null;
                DESKeySpec dks = new DESKeySpec(SOIL.getBytes());
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
                keyDesString = DatatypeConverter.printHexBinary(keyFactory.generateSecret(dks).getEncoded());
                String keyDesStringEncoded = Base64.encodeBase64String(keyDesString.getBytes(StandardCharsets.UTF_8));
                keyDes = createDESKey(keyDesStringEncoded);
                saveKey(keyDesStringEncoded);
                System.out.println("key: " + DatatypeConverter.printHexBinary(keyDes.getEncoded()) + ConsoleColors.RESET);
            } else {
                keyDes = createDESKey(keyDesString);
            }
        }
    }

    /**
     * rozkodowuje tekst <b>encodedData</b> przy pomocy algorytmu DES,
     *
     * @param encodedData zakodowany tekst za pomocą algorytmu DES
     * @return String zwraca zdekodowany tekst
     */
    public static String decodeDES(@NotNull String encodedData) {
        try {
            init();
            Cipher cipher = Cipher.getInstance("DES");
            assert keyDes != null;
            cipher.init(Cipher.DECRYPT_MODE, keyDes);
            return new String(cipher.doFinal(Base64.decodeBase64(encodedData.getBytes())));
        } catch (Exception ex) {
            System.out.println(ConsoleColors.RED_BOLD + "Nastąpił błąd: " + ex.getMessage() + ConsoleColors.RESET);
            return null;
        }
    }
}


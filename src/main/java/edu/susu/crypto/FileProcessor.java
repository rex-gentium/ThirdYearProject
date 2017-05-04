package edu.susu.crypto;

import edu.susu.database.User;
import edu.susu.exception.IdleUpdateException;

import javax.ws.rs.core.Response;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Инкапсулирует методы, отвечающие за сохранение пользовательских файлов и обработку их нейронной сетью
 */
public class FileProcessor {

    static final String STORAGE_PATH = "D:/cryptoANN/storage/";
    static final String ANN_EXECUTABLE_PATH = "D:/cryptoANN/cryptoANN.exe";

    //public enum NetworkMode { NONE, TRAIN, ENCRYPT, DECRYPT }

    public static void createUserDirectory(User user) throws IOException {
        if (user.getStoragePath() != null)
            return;
        try {
            Path userDirectory = Files.createDirectories(Paths.get(STORAGE_PATH, user.getName()));
            WebInterfaceService.db.updateUserStoragePath(user.getName(), userDirectory.toAbsolutePath().toString());
            user.setStoragePath(userDirectory.toAbsolutePath().toString());
        } catch (IdleUpdateException e) {
            e.printStackTrace();
        }
    }

    public static Path saveFileInStorage(User user, InputStream file, String fileName) {
        try {
            if (user.getStoragePath() == null)
                createUserDirectory(user);
            Path userDirectory = Paths.get(user.getStoragePath());
            Path filePath = Files.createTempFile(userDirectory, fileName, null);
            FileOutputStream fos = new FileOutputStream(filePath.toFile());
            int rByte;
            while ((rByte = file.read()) >= 0)
                fos.write(rByte);
            fos.flush();
            fos.close();
            file.close();
            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean trainNeuralNetwork(String directoryPath, String fileName) {
        try {
            Process process = new ProcessBuilder(ANN_EXECUTABLE_PATH, directoryPath, fileName, "train").start();
            int returnValue = process.waitFor();
            return (returnValue == 0);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Path encryptFile(String directoryPath, String fileName) {
        try {
            Process process = new ProcessBuilder(ANN_EXECUTABLE_PATH, directoryPath, fileName, "encrypt").start();
            int returnValue = process.waitFor();
            return (returnValue == 0) ? Paths.get(directoryPath, fileName + ".crypto") : null;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Path decryptFile(String directoryPath, String fileName) {
        try {
            Process process = new ProcessBuilder(ANN_EXECUTABLE_PATH, directoryPath, fileName, "decrypt").start();
            int returnValue = process.waitFor();
            return (returnValue == 0) ? Paths.get(directoryPath, fileName + ".decrypted") : null;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}

package cloud.storage.server.file.manager;

import cloud.storage.nio.UserData;
import cloud.storage.util.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class responsible for managing inner file system and giving access to users.
 */
@Slf4j
public class FileManager {
    private final static Duration PENALTY = Duration.ofMinutes(5);
    private final static Integer SIGN_IN_ATTEMPTS_LIMIT = 3;
    private final Map<String, String> users;
    private final Map<String, SocketAddress> sessionByUser = new HashMap<>();
    private final Map<SocketAddress, String> userBySession = new HashMap<>();
    private final Map<SocketAddress, Instant> addressPenalty = new HashMap<>();
    private final Map<SocketAddress, Integer> addressSignInAttempts = new HashMap<>();

    private final File root;
    private final Map<String, File> userRoot = new HashMap<>();
    private final Map<String, Path> userWorkingDirectory = new HashMap<>();

    public FileManager(Path root) {
        this.root = new File(root.toAbsolutePath().normalize().toUri());
        this.root.mkdirs();
        users = new HashMap<>();
    }

    private boolean addressIsBanned(SocketAddress address) {
        if (!addressPenalty.containsKey(address)) {
            return false;
        }
        Duration timePassed = Duration.between(addressPenalty.get(address), Instant.now());
        if (timePassed.minus(PENALTY).isPositive()) {
            addressPenalty.remove(address);
            return false;
        }
        return true;
    }

    private Pair<Boolean, String> startSession(SocketAddress address, String login) {
        if (sessionByUser.containsKey(login)) {
            return new Pair<>(false, "This user already has an active session.");
        }
        if (userBySession.containsKey(address)) {
            return new Pair<>(false, "This address already has an active session.");
        }

        userBySession.put(address, login);
        sessionByUser.put(login, address);

        log.info("Session started: " + address + "->" + login);

        Path userRelativeWorkingDirectory = Path.of("/").resolve(
                userRoot.get(login).toPath().relativize(userWorkingDirectory.get(login)));
        return new Pair<>(true, login + " " + userRelativeWorkingDirectory);
    }

    private void endSession(SocketAddress address) {
        if (!userBySession.containsKey(address)) {
            return;
        }
        String login = userBySession.get(address);
        userBySession.remove(address);
        sessionByUser.remove(login);

        log.info("Session ended: " + address + "->" + login);
    }

    public Pair<Boolean, String> signUp(SocketAddress address, UserData userData) {
        if (userBySession.containsKey(address)) {
            return new Pair<>(false, "There is an active session with such address already.");
        }
        if (users.containsKey(userData.getLogin())) {
            return new Pair<>(false, "User with such login is already registered.");
        }
        users.put(userData.getLogin(), userData.getPassword());

        log.info("User created: " + userData.getLogin() + " " + userData.getPassword());

        File folder = userRoot.computeIfAbsent(userData.getLogin(), this::createUserFolder);
        if (folder == null) {
            users.remove(userData.getLogin());
            log.info("User removed: " + userData.getLogin());
            return new Pair<>(false, "Failed to create user folder");
        }

        return startSession(address, userData.getLogin());
    }

    public Pair<Boolean, String> signIn(SocketAddress address, UserData userData) {
        if (addressIsBanned(address)) {
            return new Pair<>(false, "Address is still banned. Please try again later.");
        }
        addressSignInAttempts.put(address, addressSignInAttempts.getOrDefault(address, 0) + 1);
        if (addressSignInAttempts.get(address) > SIGN_IN_ATTEMPTS_LIMIT) {
            addressSignInAttempts.remove(address);
            addressPenalty.put(address, Instant.now());
            return new Pair<>(false, "Address is banned due to excessive number of signing in attempts.\n" +
                    "Please wait " + PENALTY.toMinutes() + " minutes and try again.");
        }
        if (userBySession.containsKey(address)) {
            return new Pair<>(false, "There is an active session with such address already.");
        }
        if (!users.containsKey(userData.getLogin())) {
            return new Pair<>(false, "There is no user with such login.");
        }
        if (!Objects.equals(users.get(userData.getLogin()), userData.getPassword())) {
            return new Pair<>(false, "Wrong password.");
        }
        return startSession(address, userData.getLogin());
    }

    public void signOut(SocketAddress address) {
        endSession(address);
    }

    public Pair<Boolean, String> uploadFile(SocketAddress address, Path path, InputStream inputStream) {
        String login = userBySession.get(address);
        if (login == null) {
            return new Pair<>(false, "Unknown session. Please sign up or sign in and try again.");
        }
        try {
            Pair<Path, String> resolveResult = resolveUserPath(login, path);
            if (resolveResult.getFirst() == null) {
                return new Pair<>(false, resolveResult.getSecond());
            }
            Path filePath = resolveResult.getFirst();

            new File(filePath.getParent().toUri()).mkdirs();
            Files.copy(inputStream, filePath);
            log.info("File " + filePath + " uploaded.");
        } catch (IOException e) {
            log.error("Error occurred while trying to write a file: ", e);
            return new Pair<>(false, "Error occurred while trying to write a file in cloud .");
        }
        return new Pair<>(true, null);
    }

    public Pair<InputStream, String> downloadFile(SocketAddress address, Path path) {
        String login = userBySession.get(address);
        if (login == null) {
            return new Pair<>(null, "Unknown session. Please sign up or sign in and try again.");
        }
        Pair<Path, String> resolveResult = resolveUserPath(login, path);
        if (resolveResult.getFirst() == null) {
            return new Pair<>(null, resolveResult.getSecond());
        }
        Path filePath = resolveResult.getFirst();

        File file = new File(filePath.toUri());
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            return new Pair<>(fileInputStream, null);
        } catch (FileNotFoundException ignored) {
            return new Pair<>(null, "File not found. Please check the path and try again.");
        }
    }

    public Pair<Boolean, String> moveFile(SocketAddress address, Path source, Path dest) {
        String login = userBySession.get(address);
        if (login == null) {
            return new Pair<>(null, "Unknown session. Please sign up or sign in and try again.");
        }
        Pair<Path, String> resolveResult = resolveUserPath(login, source);
        if (resolveResult.getFirst() == null) {
            return new Pair<>(null, resolveResult.getSecond());
        }
        Path sourceResolved = resolveResult.getFirst();

        resolveResult = resolveUserPath(login, dest);
        if (resolveResult.getFirst() == null) {
            return new Pair<>(null, resolveResult.getSecond());
        }
        Path destResolved = resolveResult.getFirst();

        // TODO:: handle errors?

        File sourceFile = new File(sourceResolved.toUri());
        File destFile = new File(destResolved.toUri());


        // TODO:: remove copy-paste
        if (destResolved.getParent() != null) {
            new File(destResolved.getParent().toUri()).mkdirs();
        }

        boolean success = sourceFile.renameTo(destFile);
        if (!success) {
            return new Pair<>(false, "Failed to move a file");
        }
        return new Pair<>(true, null);

    }

    private File createUserFolder(String login) {
        File file = new File(root, login);
        try {
            if (!getFilePath(file).startsWith(getFilePath(root))
                    || !file.exists() && !file.mkdir()
                    || !file.isDirectory()
                    || !file.canWrite()
                    || !file.canRead()) {
                return null;
            }
        } catch (SecurityException e) {
            return null;
        }
        userWorkingDirectory.put(login, file.toPath().toAbsolutePath().normalize());
        return file;
    }

    private static Path getFilePath(File file) {
        return file.toPath().toAbsolutePath().normalize();
    }

    private Pair<Path, String> resolveUserPath(String login, Path path) {
        path = path.normalize();

        // converting absolute path to relative by removing root
        if (path.isAbsolute()) {
            Path pathRoot = path.getRoot();
            path = pathRoot.relativize(path);
        }

        Path filePath = userWorkingDirectory.get(login).resolve(path).normalize();
        Path userRootPath = getFilePath(userRoot.get(login));
        if (!filePath.startsWith(userRootPath) || filePath.equals(userRootPath)) {
            return new Pair<>(null, "Invalid path passed. You have no access to files outside your folder.");
        }
        return new Pair<>(filePath, null);
    }
}

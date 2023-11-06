package cloud.storage.server.file.manager;

import cloud.storage.nio.UserData;
import cloud.storage.util.Pair;

import java.io.File;
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

        System.out.println("Session started: " + address + "->" + login);

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

        System.out.println("Session ended: " + address + "->" + login);
    }

    public Pair<Boolean, String> signUp(SocketAddress address, UserData userData) {
        if (userBySession.containsKey(address)) {
            return new Pair<>(false, "There is an active session with such address already.");
        }
        if (users.containsKey(userData.getLogin())) {
            return new Pair<>(false, "User with such login is already registered.");
        }
        users.put(userData.getLogin(), userData.getPassword());

        System.out.println("User created: " + userData.getLogin() + " " + userData.getPassword());

        File folder = userRoot.computeIfAbsent(userData.getLogin(), this::createUserFolder);
        if (folder == null) {
            users.remove(userData.getLogin());
            System.out.println("User removed: " + userData.getLogin());
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
            System.out.println("File " + filePath + " uploaded.");
        } catch (IOException e) {
            System.err.println("Error occurred while trying to write a file: " + e.getMessage());
            e.printStackTrace();
            return new Pair<>(false, "Error occurred while trying to write a file on server.");
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

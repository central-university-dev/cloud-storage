package cloud.storage.file.manager;

import cloud.storage.nio.UserData;
import cloud.storage.util.Pair;

import java.net.SocketAddress;
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


    public FileManager() {
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

        return new Pair<>(true, "The session began successfully.");
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
}

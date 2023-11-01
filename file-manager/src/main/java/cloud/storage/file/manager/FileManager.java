package cloud.storage.file.manager;

import cloud.storage.util.Pair;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FileManager {
    private final Map<String, String> users;
    private final Map<String, SocketAddress> sessionByUser = new HashMap<>();
    private final Map<SocketAddress, String> userBySession = new HashMap<>();


    public FileManager() {
        users = new HashMap<>();
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

    public Pair<Boolean, String> signUp(SocketAddress address, cloud.storage.data.UserData userData) {
        if (userBySession.containsKey(address)) {
            return new Pair<>(false, "There is active session with such address already.");
        }
        if (users.containsKey(userData.getLogin())) {
            return new Pair<>(false, "User with such login is already registered.");
        }
        users.put(userData.getLogin(), userData.getPassword());

        System.out.println("User created: " + userData.getLogin() + " " + userData.getPassword());

        return startSession(address, userData.getLogin());
    }

    public Pair<Boolean, String> signIn(SocketAddress address, cloud.storage.data.UserData userData) {
        if (userBySession.containsKey(address)) {
            return new Pair<>(false, "There is active session with such address already.");
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

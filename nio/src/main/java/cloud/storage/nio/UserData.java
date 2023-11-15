package cloud.storage.nio;

import cloud.storage.data.Field;

import java.nio.ByteBuffer;

/**
 * Data type containing information about user authorization.
 *
 * @see SignInResponse
 */
public class UserData implements Field {
    private final String login, password;

    public UserData(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public int getByteLength() {
        return Integer.BYTES + login.getBytes().length + Integer.BYTES + password.getBytes().length;
    }

    @Override
    public byte[] getBytes() {
        byte[] buf = new byte[getByteLength()];
        ByteBuffer.wrap(buf)
                .putInt(login.length()).put(login.getBytes())
                .putInt(password.length()).put(password.getBytes());
        return buf;
    }

    public static UserData fromBytes(ByteBuffer byteBuffer) {
        String login = stringFromBytes(byteBuffer);
        String password = stringFromBytes(byteBuffer);
        return new UserData(login, password);
    }

    private static String stringFromBytes(ByteBuffer byteBuffer) {
        int length = byteBuffer.getInt();
        byte[] stringBytes = new byte[length];
        byteBuffer.get(stringBytes);
        return new String(stringBytes);
    }

    @Override
    public String toString() {
        return "User " + login;
    }
}

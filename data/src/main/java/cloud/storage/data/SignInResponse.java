package cloud.storage.data;

import java.nio.ByteBuffer;

public class SignInResponse implements Field {
    private enum Status {
        SUCCESS((byte) 1),
        FAILURE((byte) 2);

        private final byte val;

        Status(byte val) {
            this.val = val;
        }

        private static Status getStatus(byte val) {
            for (Status s : Status.values()) {
                if (s.val == val) {
                    return s;
                }
            }
            throw new RuntimeException("Unknown status");

        }
    }

    public final Status status;
    public final UserData userData;
    public final String message;

    private SignInResponse(Status status, UserData userData) {
        this(status, userData, null);
    }

    private SignInResponse(Status status, String message) {
        this(status, null, message);
    }

    private SignInResponse(Status status, UserData userData, String message) {
        this.status = status;
        this.userData = userData;
        this.message = message;
    }


    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public static SignInResponse success(UserData userData) {
        return new SignInResponse(Status.SUCCESS, userData);
    }

    public static SignInResponse failure(String message) {
        return new SignInResponse(Status.FAILURE, message);
    }

    public static SignInResponse fromBytes(ByteBuffer byteBuffer) {
        Status status = Status.getStatus(byteBuffer.get());
        if (status == Status.SUCCESS) {
            UserData userData = UserData.fromBytes(byteBuffer);
            return success(userData);
        }
        String message = null;
        if (byteBuffer.hasRemaining()) {
            int messageLength = byteBuffer.getInt();
            byte[] messageBytes = new byte[messageLength];
            byteBuffer.get(messageBytes);
            message = new String(messageBytes);
        }
        return failure(message);
    }

    @Override
    public int getLength() {
        if (status == Status.SUCCESS) {
            return 1 + userData.getLength();
        }
        return 1 + (message != null ? Integer.BYTES + message.length() : 0);
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = new byte[getLength()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(status.val);
        if (status == Status.SUCCESS) {
            byteBuffer.put(userData.getBytes());
        } else {
            if (message != null) {
                byteBuffer.putInt(message.length()).put(message.getBytes());
            }
        }
        return bytes;
    }
}

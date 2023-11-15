package cloud.storage.nio;

import cloud.storage.data.Field;

import java.nio.ByteBuffer;

/**
 * Data type containing server response of SignIn command.
 */
public class SignInResponse implements Field {
    private enum Status {
        SUCCESS((byte) 1),
        FAILURE((byte) 2);

        static private final int BYTE_LENGTH = 1;

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

    private final Status status;
    private final String message;

    private SignInResponse(Status status, String userRoot) {
        this.status = status;
        this.message = userRoot;
    }


    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public static SignInResponse success(String userRoot) {
        return new SignInResponse(Status.SUCCESS, userRoot);
    }

    public static SignInResponse failure(String message) {
        return new SignInResponse(Status.FAILURE, message);
    }

    public String getMessage() {
        return message;
    }

    public static SignInResponse fromBytes(ByteBuffer byteBuffer) {
        Status status = Status.getStatus(byteBuffer.get());
        int length = byteBuffer.getInt();
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        String stringResponse = new String(bytes);
        if (status == Status.SUCCESS) {
            return success(stringResponse);
        }
        return failure(stringResponse);
    }

    @Override
    public int getByteLength() {
        return Status.BYTE_LENGTH + (message != null ? Integer.BYTES + message.length() : 0);
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = new byte[getByteLength()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(status.val);
        byteBuffer.putInt(message.length()).put(message.getBytes());
        return bytes;
    }
}

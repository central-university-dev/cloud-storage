package cloud.storage.data;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

/**
 * Enum of all the existing commands.
 *
 * @see Payload
 */
public enum Cmd implements Field {
    MESSAGE((byte) 0),

    PING((byte) 1),

    TIME((byte) 2),

    SIGN_UP((byte) 3),

    SIGN_IN((byte) 4),

    SIGN_OUT((byte) 5),

    UPLOAD((byte) 6),

    DOWNLOAD((byte) 7),
    MOVE((byte) 8);

    static private final int BYTE_LENGTH = 1;
    private final byte val;

    Cmd(byte val) {
        this.val = val;
    }

    @Override
    public int getByteLength() {
        return BYTE_LENGTH;
    }

    @Override
    public byte[] getBytes() {
        return new byte[]{val};
    }

    public static Cmd readCmd(ByteBuf in) {
        byte[] val = new byte[BYTE_LENGTH];
        in.readBytes(val);
        for (Cmd c : Cmd.values()) {
            if (Arrays.equals(c.getBytes(), val)) {
                return c;
            }
        }
        throw new RuntimeException("Unknown cmd");
    }

    public static Cmd getCmd(String name) {
        name = name.toUpperCase();
        for (Cmd c : Cmd.values()) {
            String formatted = String.join("", c.name().split("_"));
            if (formatted.equals(name)) {
                return c;
            }
        }
        throw new RuntimeException("Unknown cmd");
    }
}

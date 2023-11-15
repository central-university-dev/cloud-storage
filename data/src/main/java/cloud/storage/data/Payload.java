package cloud.storage.data;

import java.nio.ByteBuffer;

/**
 * Data class containing information about command and inner data.
 */
public class Payload implements Field {
    public final Cmd cmd;
    public final byte[] cmdBody;


    public Payload(Cmd cmd, byte[] cmdBody) {
        this.cmd = cmd;
        this.cmdBody = cmdBody;
    }

    @Override
    public int getByteLength() {
        return cmd.getByteLength() + (cmdBody != null ? cmdBody.length : 0);
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = new byte[getByteLength()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(cmd.getBytes());
        if (cmdBody != null) {
            byteBuffer.put(cmdBody);
        }

        return bytes;
    }

    @Override
    public String toString() {
        return "Payload{" +
                "cmd=" + cmd.name() +
                '}';
    }
}

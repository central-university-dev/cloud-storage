package cloud.storage.data;

import java.nio.ByteBuffer;

public class Packet implements Field {
    public final int length;
    public final Payload payload;

    public Packet(Payload payload) {
        this(payload.getLength(), payload);
    }

    private Packet(int length, Payload payload) {
        this.length = length;
        this.payload = payload;
    }

    @Override
    public int getLength() {
        return 4 + length;
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = new byte[getLength()];
        ByteBuffer.wrap(bytes)
                .putInt(length)
                .put(payload.getBytes());
        return bytes;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "length=" + length +
                ", payload=" + payload +
                '}';
    }
}


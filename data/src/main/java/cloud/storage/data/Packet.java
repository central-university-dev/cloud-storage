package cloud.storage.data;

import java.nio.ByteBuffer;

/**
 * Primary data packet used for network communication.
 *
 * @see Payload
 */
public class Packet implements Field {
    public final int payloadByteLength;
    public final Payload payload;

    public Packet(Payload payload) {
        this(payload.getByteLength(), payload);
    }

    private Packet(int payloadByteLength, Payload payload) {
        this.payloadByteLength = payloadByteLength;
        this.payload = payload;
    }

    @Override
    public int getByteLength() {
        return Integer.BYTES + payloadByteLength;
    }

    @Override
    public byte[] getBytes() {
        byte[] bytes = new byte[getByteLength()];
        ByteBuffer.wrap(bytes)
                .putInt(payloadByteLength)
                .put(payload.getBytes());
        return bytes;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "length=" + payloadByteLength +
                ", payload=" + payload +
                '}';
    }
}


package cloud.storage.data;

import java.nio.ByteBuffer;
import java.util.Date;

public class TimeData implements Field {
    private final long timestamp;
    private final Date date;

    public TimeData() {
        this(System.currentTimeMillis());
    }

    public TimeData(long timestamp) {
        this.timestamp = timestamp;
        this.date = new Date(timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return date.toString();
    }

    @Override
    public int getLength() {
        return Long.BYTES;
    }

    @Override
    public byte[] getBytes() {
        return ByteBuffer.allocate(getLength()).putLong(timestamp).array();
    }

    public static TimeData fromBytes(byte[] bytes) {
        return new TimeData(ByteBuffer.wrap(bytes).getLong());
    }
}

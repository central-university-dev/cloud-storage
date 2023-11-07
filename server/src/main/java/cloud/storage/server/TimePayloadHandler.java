package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.nio.TimeData;
import io.netty.channel.ChannelHandlerContext;

/**
 * Class for server side inbound time payloads handling.
 */
public class TimePayloadHandler implements PayloadHandler {
    private static final Cmd CMD = Cmd.TIME;

    private static Packet getPacket(byte[] cmbBody) {
        return new Packet(new Payload(CMD, cmbBody));
    }

    /**
     * Sends current time on this machine.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, Payload payload) {
        context.writeAndFlush(getPacket((new TimeData()).getBytes()));
    }
}

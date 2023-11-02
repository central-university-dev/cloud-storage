package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Class for server side inbound ping payloads handling.
 */
public class PingPayloadHandler implements PayloadHandler {
    private static final Cmd CMD = Cmd.PING;

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    /**
     * Responses with the same payload data as received.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        context.writeAndFlush(getPacket(cmdBody));
    }
}

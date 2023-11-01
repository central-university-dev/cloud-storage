package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;

public class PingPayloadHandler implements PayloadHandler {
    private static final Cmd CMD = Cmd.PING;

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        context.writeAndFlush(getPacket(cmdBody));
    }
}

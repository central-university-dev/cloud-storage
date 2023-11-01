package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.data.TimeData;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;

public class TimePayloadHandler implements PayloadHandler {
    private static final Cmd CMD = Cmd.TIME;

    private static Packet getPacket(byte[] cmbBody) {
        return new Packet(new Payload(CMD, cmbBody));
    }

    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        context.writeAndFlush(getPacket((new TimeData()).getBytes()));
    }
}

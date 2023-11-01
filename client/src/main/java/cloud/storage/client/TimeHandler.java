package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.nio.CommandHandler;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.data.TimeData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

public class TimeHandler implements CommandHandler, PayloadHandler {
    private static final Cmd CMD = Cmd.TIME;

    private static Packet getPacket() {
        return new Packet(new Payload(CMD, null));
    }

    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        context.write(getPacket(), promise);
    }

    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        context.fireChannelRead(TimeData.fromBytes(cmdBody).toString());
    }
}

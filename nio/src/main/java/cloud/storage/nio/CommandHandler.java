package cloud.storage.nio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

public interface CommandHandler {
    void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise);
}

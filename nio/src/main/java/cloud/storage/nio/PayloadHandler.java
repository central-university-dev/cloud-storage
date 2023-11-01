package cloud.storage.nio;

import io.netty.channel.ChannelHandlerContext;

public interface PayloadHandler {
    void handle(ChannelHandlerContext context, byte[] cmdBody);
}

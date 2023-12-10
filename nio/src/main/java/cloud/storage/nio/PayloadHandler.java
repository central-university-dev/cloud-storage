package cloud.storage.nio;

import cloud.storage.data.Payload;
import io.netty.channel.ChannelHandlerContext;

/**
 * Interface for inbound payload handlers
 *
 * @see CommandHandler
 */
public interface PayloadHandler {

    /**
     * Handles the inbound payload.
     *
     * @param context context which got the payload.
     * @param payload payload to handle.
     */
    void handle(ChannelHandlerContext context, Payload payload);
}

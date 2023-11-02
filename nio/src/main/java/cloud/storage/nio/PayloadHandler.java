package cloud.storage.nio;

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
     * @param cmdBody data of the payload to handle.
     */
    void handle(ChannelHandlerContext context, byte[] cmdBody);
}

package cloud.storage.nio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

/**
 * Interface for outbound command handlers
 *
 * @see PayloadHandler
 */
public interface CommandHandler {
    /**
     * Executes the outbound command.
     *
     * @param context   context in which to execute the command
     * @param arguments command arguments
     * @param promise   promise to monitor the execution status of the command
     */
    void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise);
}

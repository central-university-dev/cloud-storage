package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.nio.CommandHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.Map;

/**
 * Handles user commands and send the request on the server.
 *
 * @see ClientHandler
 * @see CommandParser
 */
class ChannelCommandHandler extends ChannelOutboundHandlerAdapter {
    /**
     * Map of outbound command handlers instances
     *
     * @see CommandHandler
     */
    private final Map<Cmd, ? extends CommandHandler> COMMAND_HANDLERS;

    ChannelCommandHandler(ClientHandler clientHandler) {
        super();

        COMMAND_HANDLERS = Map.of(
                Cmd.PING, new PingHandler(),
                Cmd.TIME, new TimeHandler(),
                Cmd.SIGN_UP, new SignUpHandler(),
                Cmd.SIGN_IN, new SignInHandler(clientHandler),
                Cmd.SIGN_OUT, new SignOutHandler(clientHandler)
        );
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof CommandMessage commandMessage) {
            CommandHandler commandHandler = COMMAND_HANDLERS.get(commandMessage.getFirst());
            if (commandHandler == null) {
                promise.setFailure(new IllegalArgumentException("Cant handle command " + commandMessage.getFirst().name()));
                return;
            }
            commandHandler.execute(ctx, commandMessage.getSecond(), promise);
        } else {
            ctx.write(msg, promise);
        }
    }

}

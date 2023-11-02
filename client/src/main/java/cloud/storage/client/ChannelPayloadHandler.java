package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;

/**
 * Handles responses from the server.
 *
 * @see ClientHandler
 */
class ChannelPayloadHandler extends ChannelInboundHandlerAdapter {
    /**
     * Map of inbound payload handlers instances
     *
     * @see PayloadHandler
     */
    private final Map<Cmd, ? extends PayloadHandler> PAYLOAD_HANDLERS;

    ChannelPayloadHandler(ClientHandler clientHandler) {

        PAYLOAD_HANDLERS = Map.of(
                Cmd.PING, new PingHandler(),
                Cmd.TIME, new TimeHandler(),
                Cmd.SIGN_UP, new SignUpHandler(),
                Cmd.SIGN_IN, new SignInHandler(clientHandler),
                Cmd.SIGN_OUT, new SignOutHandler(clientHandler)
        );
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Payload payload) {
            PayloadHandler payloadHandler = PAYLOAD_HANDLERS.get(payload.cmd);
            if (payloadHandler == null) {
                System.err.println("Cant handle payload with cmd " + payload.cmd.name());
                return;
            }
            payloadHandler.handle(ctx, payload.cmdBody);
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}

package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.server.file.manager.FileManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

/**
 * Main logic of server side application.
 * Receives the client's requests, handles it and sends a response.
 */
class RequestHandler extends SimpleChannelInboundHandler<Payload> {
    private final Map<Cmd, ? extends PayloadHandler> REQUEST_HANDLER_INSTANCES;

    private final FileManager fileManager;

    RequestHandler(FileManager fileManager) {
        this.fileManager = fileManager;

        REQUEST_HANDLER_INSTANCES = Map.of(
                Cmd.PING, new PingPayloadHandler(),
                Cmd.TIME, new TimePayloadHandler(),
                Cmd.SIGN_UP, new SignUpHandler(fileManager),
                Cmd.SIGN_IN, new SignInHandler(fileManager),
                Cmd.SIGN_OUT, new SignOutHandler(fileManager),
                Cmd.UPLOAD, new UploadHandler(fileManager),
                Cmd.DOWNLOAD, new DownloadHandler(fileManager),
                Cmd.MOVE, new MoveHandler(fileManager)
        );
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        fileManager.signOut(ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Payload msg) {
        PayloadHandler requestHandler = REQUEST_HANDLER_INSTANCES.get(msg.cmd);
        if (requestHandler == null) {
            System.err.println("Got unknown cmd: " + msg.cmd);
            ctx.write(new Packet(new Payload(Cmd.PING, "The server can't handle this command.".getBytes())));
            return;
        }
        requestHandler.handle(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

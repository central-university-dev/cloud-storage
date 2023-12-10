package cloud.storage.server;

import cloud.storage.nio.PacketEncoder;
import cloud.storage.nio.PayloadDecoder;
import cloud.storage.nio.ReplayingPacketDecoder;
import cloud.storage.server.file.manager.FileManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Server side of application.
 * Handles user requests and manages the inner file system.
 */
@Slf4j
public class Server {
    private final Path root;
    private final int port;

    /**
     * Initiates a server.
     *
     * @param port port to connect server to.
     */
    public Server(Path root, int port) {
        this.root = root;
        this.port = port;
    }

    /**
     * Runs the server.
     * This method does not return immediately but when the server shut down.
     */
    public void run() {
        FileManager fileManager = new FileManager(root);

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        EventExecutorGroup businessGroup = new UnorderedThreadPoolEventExecutor(4);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // TODO:: make PacketEncoder MessageToByteEncoder<Payload> and change it in every handler
            // TODO:: make all messages (e.g. errors) be Cmd.MESSAGE payloads
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(@NotNull SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast("ReplayingPacketDecoder", new ReplayingPacketDecoder());
                            pipeline.addLast("PayloadDecoder", new PayloadDecoder());

                            pipeline.addLast("PacketEncoder", new PacketEncoder());

                            pipeline.addLast(businessGroup, "requestHandler", new RequestHandler(fileManager));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture serverBootstrapBindFuture = serverBootstrap.bind(port);
            log.info("Server was bound to the port {}", port);
            serverBootstrapBindFuture = serverBootstrapBindFuture.sync();

            serverBootstrapBindFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Server's thread was interrupted: ", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * Another way to run the server.
     *
     * @param args args[0] -- root folder path, args[1] -- port to connect server to.
     */
    public static void main(String[] args) {
        Path root;
        int port;
        try {
            if (args.length == 2) {
                root = Path.of(args[0]);
                port = Integer.parseInt(args[1]);
            } else {
                log.error("You have to pass the root folder path and the port number as arguments");
                return;
            }
        } catch (InvalidPathException e) {
            log.error("Invalid root folder path passed: ", e);
            return;
        } catch (NumberFormatException ignored) {
            log.error("Port number must be an integer");
            return;
        }

        new Server(root, port).run();
    }
}

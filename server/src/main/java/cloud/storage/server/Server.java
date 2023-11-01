package cloud.storage.server;

import cloud.storage.file.manager.FileManager;
import cloud.storage.nio.PacketEncoder;
import cloud.storage.nio.PayloadDecoder;
import cloud.storage.nio.ReplayingPacketDecoder;
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
import org.jetbrains.annotations.NotNull;

public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public void run() throws InterruptedException {
        FileManager fileManager = new FileManager();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        EventExecutorGroup businessGroup = new UnorderedThreadPoolEventExecutor(4);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(@NotNull SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast("ReplayingPacketDecoder", new ReplayingPacketDecoder());
                            p.addLast("PayloadDecoder", new PayloadDecoder());

                            p.addLast("PacketEncoder", new PacketEncoder());

                            p.addLast(businessGroup, "requestHandler", new RequestHandler(fileManager));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync(); // (7)

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int port;
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            } else {
                System.err.println("You have to pass the port number as argument");
                return;
            }
        } catch (NumberFormatException ignored) {
            System.err.println("Port number must be an integer");
            return;
        }

        new Server(port).run();
    }
}

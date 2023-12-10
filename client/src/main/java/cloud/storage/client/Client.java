package cloud.storage.client;

import cloud.storage.nio.PacketEncoder;
import cloud.storage.nio.PayloadDecoder;
import cloud.storage.nio.ReplayingPacketDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

/**
 * Client-side application to connect and work with server.
 * Connects to server, gets the user commands, sends it to server and handles response.
 */
@Slf4j
public class Client {
    static final String CLIENT_HANDLER_NAME = "userInteraction";

    /**
     * The general way for user to interact with the application.
     *
     * @param args args[0]:args[1] -- server remote address.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            log.error("You have to pass host and port as arguments.");
            return;
        }
        String host;
        int port;
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            log.error("Port must be a number");
            return;
        }
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try (Reader reader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader);
             Writer writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)
        ) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            // TODO:: make PacketEncoder MessageToByteEncoder<Payload> and change it in every handler
            // TODO:: make all messages (e.g. errors) be Cmd.MESSAGE payloads
            // TODO:: make String be encodable and decodable easily, remove all copy-paste with that
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("ReplayingPacketDecoder", new ReplayingPacketDecoder());
                    pipeline.addLast("PayloadDecoder", new PayloadDecoder());

                    pipeline.addLast("PacketEncoder", new PacketEncoder());

                    pipeline.addLast(CLIENT_HANDLER_NAME, new ClientHandler(bufferedReader, bufferedWriter));
                }
            });

            ChannelFuture bootstrapConnectFuture = bootstrap.connect(host, port).sync();

            bootstrapConnectFuture.channel().closeFuture().sync();
        } catch (ConnectException e) {
            log.error("Failed to connect to server: ", e);
        } catch (IOException e) {
            log.error("Error occurred with input/output stream: ", e);
        } catch (InterruptedException e) {
            log.error("Main thread was interrupted: ", e);
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

}

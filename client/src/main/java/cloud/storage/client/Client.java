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
public class Client {

    /**
     * The general way for user to interact with the application.
     *
     * @param args args[0]:args[1] -- server remote address.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("You have to pass host and port as arguments.");
        }
        String host;
        int port;
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Port must be a number");
            return;
        }
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try (Reader reader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader);
             Writer writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)
        ) {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast("ReplayingPacketDecoder", new ReplayingPacketDecoder());
                    p.addLast("PayloadDecoder", new PayloadDecoder());

                    p.addLast("PacketEncoder", new PacketEncoder());

                    ClientHandler clientHandler = new ClientHandler(bufferedReader, bufferedWriter);
                    p.addLast("inboundPacketHandler", new ChannelPayloadHandler(clientHandler));

                    p.addLast("outboundCommandHandler", new ChannelCommandHandler(clientHandler));
                    p.addLast("commandParser", new CommandParser());

                    p.addLast("userInteraction", clientHandler);
                }
            });

            ChannelFuture f = b.connect(host, port).sync();

            f.channel().closeFuture().sync();
        } catch (ConnectException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error occurred with input/output stream: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Main thread was interrupted: " + e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

}

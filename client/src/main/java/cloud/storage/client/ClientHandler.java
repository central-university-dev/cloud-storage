package cloud.storage.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Primary handler on client-side application.
 * Responsible for user interaction: receiving commands and returning messages.
 *
 * @see CommandParser
 * @see ChannelCommandHandler
 * @see ChannelPayloadHandler
 */
class ClientHandler extends ChannelInboundHandlerAdapter {
    private final ExecutorService executorService;
    private volatile ChannelHandlerContext ctx;
    private volatile BufferedReader reader;
    private volatile BufferedWriter writer;
    private volatile String workingDirectory;

    /**
     * @param reader -- where to read user commands
     * @param writer -- where to print messages to user
     */

    ClientHandler(BufferedReader reader, BufferedWriter writer) {
        super();
        this.reader = reader;
        this.writer = writer;
        this.executorService = Executors.newFixedThreadPool(1);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        startWorking();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.ctx = null;
        shutdown();
    }

    private void startWorking() {
        println("Connected to server.");
        sendCommand();
    }

    /**
     * Gets user command from passed reader and sends it down the pipeline.
     */
    private void sendCommand() {
        executorService.submit(() -> {
            print("<" + (workingDirectory == null ? "" : workingDirectory) + ">:");

            String commandMessage = readLine();
            if (commandMessage == null) {
                shutdown();
                return;
            }
            if (commandMessage.equals("exit")) {
                shutdown();
                return;
            }
            if (commandMessage.equals("help")) {
                printHelp();
                return;
            }
            ChannelFuture future = ctx.writeAndFlush(commandMessage);
            future.addListener(sendNextCommand);
        });
    }

    private void printHelp() {
        println("<> means that you are not signed in yet.");
        println("<username> means your username.");
        println("Available commands:");
        println("ping words...");
        println("\tServer will reply with the same message");
        println("time");
        println("\tGet the server time");
        println("signUp login password");
        println("\tTry to sign up on the server with passed login and password.");
        println("signIn login password");
        println("\tTry to sign in on the server with passed login and password.");
        println("signOut");
        println("\tSign out from server.");
        println("exit");
        println("\tShutdown client");
        println("help");
        println("\tShow this message");
    }

    /**
     * Responsible to continue working in case of failure of the previous command.
     */
    private final ChannelFutureListener sendNextCommand = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            if (future.isCancelled()) {
                System.err.println("Command was cancelled");
            } else if (future.cause() != null) {
                System.err.println("Command ended with failure: " + future.cause().getMessage());
            }
            sendCommand();
        }
    };

    private void shutdown() {
        println("Shutting down");
        if (ctx != null) {
            ctx.channel().close();
        }
        executorService.shutdownNow();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof String string) {
            println(string);
            print("<" + (workingDirectory == null ? "" : workingDirectory) + ">:");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private String readLine() {
        if (reader == null) {
            return null;
        }
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("Error occurred while trying to read new line: " + e);
            System.err.println(Arrays.toString(e.getStackTrace()));
            reader = null;
            return null;
        }
    }

    private void print(String message) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error occurred while trying to print a message: " + e);
            System.err.println(Arrays.toString(e.getStackTrace()));
            writer = null;
        }
    }

    private void println(String message) {
        print(message);
        print(System.lineSeparator());
    }

    /**
     * Remembers the user's login which was used to authorize on the server
     *
     * @param login - the user's login that used to authorize on the server
     */
    protected void setWorkingDirectory(String login) {
        this.workingDirectory = login;
    }

    /**
     * Forgets the user's login from server side.
     */
    protected void resetWorkingDirectory() {
        this.workingDirectory = null;
    }
}

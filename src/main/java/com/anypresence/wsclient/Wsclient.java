package com.anypresence.wsclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.impl.SLF4JLog;
import org.apache.logging.log4j.*;


public class Wsclient implements Runnable {
    static Logger log = LogManager.getLogger(Wsclient.class.getName());
    
    private static final int EXIT_CODE_BAD_ARGS = 1;
    private static final int EXIT_CODE_UNABLE_TO_BIND = 2;

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 19083;

    private String host;
    private int port;
    private ExecutorService pool;

    public Wsclient(String host, int port, int workerPoolSize) {
        this.host = host;
        this.port = port;

        if (workerPoolSize > 0) {
            this.pool = Executors.newFixedThreadPool(workerPoolSize);
        }
    }

    public void run() {
        ServerSocket server = null;
        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(host, port));
        } catch (IOException e) {
            Log.info("Unable to bind to socket due to IOException: " + e.getMessage() + ".  Exiting with code "
                    + EXIT_CODE_UNABLE_TO_BIND);
            if (Log.isDebugEnabled()) {
                e.printStackTrace(System.out);
            }
            System.exit(EXIT_CODE_UNABLE_TO_BIND);
        }

        final ServerSocket finalServer = server;
        Thread hook = new Thread(() -> {
            Log.debug("JVM received shutdown signal");
            if (finalServer != null) {
                try {
                    finalServer.close();
                } catch (IOException e) {
                    // Ignore - we're shutting down anyway
                }
            }

            if (pool == null) {
                return;
            }

            pool.shutdown();

            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                        Log.info("Pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }

            Log.debug("JVM shutdown complete");
        });
        Runtime.getRuntime().addShutdownHook(hook);

        while (true) {
            Socket sock;
            try {
                sock = finalServer.accept();
            } catch (IOException e) {
                Log.info("Unable to accept socket connection! " + e.getMessage());
                if (Log.isDebugEnabled()) {
                    e.printStackTrace(System.out);
                }
                continue;
            }

            CxfWorker worker = new CxfWorker(sock);
            if (pool == null) {
                new Thread(worker).run();
            } else {
                pool.execute(worker);
            }
        }
    }

    public static void main(String[] args) {
        String host = null;
        Integer port = null;
        Integer workerPoolSize = null;


        if (args.length == 3) {
            host = args[0];
            port = Integer.parseInt(args[1]);
            workerPoolSize = Integer.parseInt(args[2]);
        } else if (args.length == 0) {
            host = DEFAULT_HOST;
            port = DEFAULT_PORT;
            workerPoolSize = 0;
        } else {
            System.out.println("Expected either 0 arguments or 3.  Exiting with code " + EXIT_CODE_BAD_ARGS);
            System.exit(EXIT_CODE_BAD_ARGS);
        }


        Wsclient main = new Wsclient(host, port, workerPoolSize);
        main.run();
    }

}

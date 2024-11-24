package com.vogella.languageserver.asciidoc;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

public class MainOnSockets {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        startServer(1200); // Start the server on port 1200
    }

    public static void startServer(int port) throws InterruptedException, ExecutionException, IOException {
        AsciidocLanguageServer server = new AsciidocLanguageServer();
        
        // Create a ServerSocket that listens on the given port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            
            // Wait for client connection
            Socket socket = serverSocket.accept();
            System.out.println("Client connected: " + socket.getInetAddress());
            
            // Get input/output streams for the socket
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // Start the language server over the socket streams
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);
            Future<?> startListening = launcher.startListening();
            server.setRemoteProxy(launcher.getRemoteProxy());

            // Wait for the server to finish processing
            startListening.get();
        }
    }
}

package com.vogella.dartlanguageserver;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class SocketStreamConnectionProvider implements StreamConnectionProvider {
    private Socket socket;
    private static final String HOST = "localhost";
    private static final int PORT = 1200;

    @Override
    public void start() throws IOException {
        // Initialize the socket connection to the language server
        socket = new Socket(HOST, PORT);
    }

    @Override
    public InputStream getInputStream() {
        // Return the input stream of the socket
        try {
			return socket.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        throw new RuntimeException();
    }

    @Override
    public OutputStream getOutputStream() {
        // Return the output stream of the socket
        try {
			return socket.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        throw new RuntimeException();

    }

    @Override
    public void stop() {
        // Close the socket when stopping the connection provider
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	@Override
	public InputStream getErrorStream() {
		// TODO Auto-generated method stub
		return null;
	}


}

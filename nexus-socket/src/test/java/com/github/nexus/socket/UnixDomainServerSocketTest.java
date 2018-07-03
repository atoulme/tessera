package com.github.nexus.socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

public class UnixDomainServerSocketTest {

    private Path socketFile = Paths.get(System.getProperty("java.io.tempdir"), "junit.txt");

    private Socket socket;

    private UnixDomainServerSocket unixDomainServerSocket;

    @Before
    public void setUp() {
        this.socket = mock(Socket.class);
        this.unixDomainServerSocket = new UnixDomainServerSocket(socket);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(socket);
    }

    @Test
    public void connectAndRead() throws IOException {

        final String data = "HELLOW-99";

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());

        doReturn(inputStream).when(socket).getInputStream();

        final byte[] result = unixDomainServerSocket.read();

        //TODO: Verify that the right padding is correct behaviour
        assertThat(new String(result)).startsWith(data);

        verify(socket).getInputStream();
    }

    @Test
    public void connectAndWrite() throws IOException {

        final String data = "HELLOW-99";

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        doReturn(outputStream).when(socket).getOutputStream();

        unixDomainServerSocket.write(data.getBytes());

        assertThat(data).isEqualTo(new String(outputStream.toByteArray()));

        verify(socket).getOutputStream();
    }

    @Test
    public void connectAndReadThrowsException() throws IOException {

        doThrow(IOException.class).when(socket).getInputStream();

        final Throwable ex = catchThrowable(unixDomainServerSocket::read);
        assertThat(ex).isInstanceOf(NexusSocketException.class).hasCauseExactlyInstanceOf(IOException.class);

        verify(socket).getInputStream();
    }

    @Test
    public void connectAndWriteThrowsException() throws IOException {

        doThrow(IOException.class).when(socket).getOutputStream();

        final Throwable ex = catchThrowable(() -> unixDomainServerSocket.write("HELLOW".getBytes()));
        assertThat(ex).isInstanceOf(NexusSocketException.class).hasCauseExactlyInstanceOf(IOException.class);

        verify(socket).getOutputStream();
    }
}

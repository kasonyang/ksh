package site.kason.ksh;

import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.DirectConnection;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Parameters;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder;
import net.schmizz.sshj.connection.channel.forwarded.SocketForwardingConnectListener;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.ConsoleKnownHostsVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.xfer.LocalFileFilter;
import net.schmizz.sshj.xfer.LocalSourceFile;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;

/**
 * @author KasonYang
 */
public class SshClient {

    protected final SSHClient sc = new SSHClient();


    public void connect(String hostname, int port) throws IOException {
        sc.connect(hostname, port);
    }

    public void connect(SshClient proxy, String hostname, int port) throws IOException {
        DirectConnection dc = proxy.sc.newDirectConnection(hostname, port);
        sc.connectVia(dc);
    }

    public void addHostKeyVerifier(String fingerprint) {
        sc.addHostKeyVerifier(fingerprint);
    }

    public void disableHostKeyVerify() {
        sc.addHostKeyVerifier(new PromiscuousVerifier());
    }

    public void addConsoleHostKeyVerifier(File khFile) throws IOException {
        sc.addHostKeyVerifier(new ConsoleKnownHostsVerifier(khFile, System.console()));
    }

    public void addConsoleHostKeyVerifier() throws IOException {
        File khFile = new File(OpenSSHKnownHosts.detectSSHDir(), "known_hosts");
        addConsoleHostKeyVerifier(khFile);
    }

    public void authPassword(String username, String password) throws UserAuthException, TransportException {
        sc.authPassword(username, password);
    }

    public void authPublicKey(String username, String... locations) throws UserAuthException, TransportException {
        sc.authPublickey(username, locations);
    }

    public void authPublicKey(String username) throws UserAuthException, TransportException {
        sc.authPublickey(username);
    }

    public boolean isConnected() {
        return sc.isConnected();
    }

    public void loadKnownHosts(File location) throws IOException {
        sc.loadKnownHosts(location);
    }

    public void disconnect() throws IOException {
        sc.disconnect();
    }

    public CommandResult exec(String command) throws ConnectionException, TransportException {
        try (Session sess = sc.startSession()) {
            Session.Command cmd = sess.exec(command);
            cmd.join();
            return new CommandResult(cmd);
        }
    }

    public LocalPortForwardResult forwardLocalPort(String localHost, int localPort, String remoteHostName, int remotePort) throws IOException {
        Parameters params = new Parameters(localHost, localPort, remoteHostName, remotePort);
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(params.getLocalHost(), params.getLocalPort()));
        LocalPortForwarder forwarder = sc.newLocalPortForwarder(params, ss);
        Thread forwardThread = new Thread(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                try {
                    forwarder.listen();
                } finally {
                    forwarder.close();
                    ss.close();
                }
            }
        });
        forwardThread.start();
        return new LocalPortForwardResult(forwardThread);
    }

    public RemotePortForwardResult forwardRemotePort(int remotePort, String host, int port) throws ConnectionException, TransportException {
        RemotePortForwarder forwarder = sc.getRemotePortForwarder();
        SocketForwardingConnectListener sfcl = new SocketForwardingConnectListener(new InetSocketAddress(host, port));
        RemotePortForwarder.Forward forward = new RemotePortForwarder.Forward(remotePort);
        forwarder.bind(forward, sfcl);
        return new RemotePortForwardResult(forwarder, forward);
    }

    public void uploadFile(String destPath, String... localPath) throws IOException {
        try (SFTPClient sftp = sc.newSFTPClient()) {
            for (String lp : localPath) {
                sftp.put(lp, destPath);
            }
        }
    }

    public void uploadBytes(String destPath, byte[] content, int permissions) throws IOException {
        String name = FilenameUtils.getName(destPath);
        BytesLocalFile source = new BytesLocalFile(name, content, permissions);
        try (SFTPClient sftp = sc.newSFTPClient()){
            sftp.put(source, destPath);
        }
    }

    public void uploadBytes(String destPath, byte[] content) throws IOException {
        uploadBytes(destPath, content, 0644);
    }

    public void downloadFile(String localPath, String... remoteFiles) throws IOException {
        try (SFTPClient sftp = sc.newSFTPClient()) {
            for (String rf : remoteFiles) {
                sftp.get(rf, localPath);
            }
        }
    }

    public static class CommandResult {
        private Session.Command cmd;

        private CommandResult(Session.Command cmd) {
            this.cmd = cmd;
        }

        public int getExitStatus() {
            return cmd.getExitStatus();
        }

        public String getOutput(String charset) throws IOException {
            return IOUtils.readFully(cmd.getInputStream()).toString(charset);
        }

        public String getError(String charset) throws IOException {
            return IOUtils.readFully(cmd.getErrorStream()).toString(charset);
        }
    }

    public static class LocalPortForwardResult {

        private Thread thread;

        public LocalPortForwardResult(Thread thread) {
            this.thread = thread;
        }

        public void close() {
            thread.interrupt();
        }

        public void join() throws InterruptedException {
            thread.join();
        }

    }

    public static class RemotePortForwardResult {
        private RemotePortForwarder forwarder;
        private RemotePortForwarder.Forward forward;

        public RemotePortForwardResult(RemotePortForwarder forwarder, RemotePortForwarder.Forward forward) {
            this.forwarder = forwarder;
            this.forward = forward;
        }

        public void close() throws ConnectionException, TransportException {
            forwarder.cancel(forward);
        }
    }

    private static class BytesLocalFile implements LocalSourceFile {

        private String name;

        private byte[] content;

        private int permissions;

        public BytesLocalFile(String name, byte[] content, int permissions) {
            this.name = name;
            this.content = content;
            this.permissions = permissions;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getLength() {
            return content.length;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public int getPermissions() throws IOException {
            return permissions;
        }

        @Override
        public boolean isFile() {
            return true;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public Iterable<? extends LocalSourceFile> getChildren(LocalFileFilter filter) throws IOException {
            return Collections.emptyList();
        }

        @Override
        public boolean providesAtimeMtime() {
            return false;
        }

        @Override
        public long getLastAccessTime() throws IOException {
            return 0;
        }

        @Override
        public long getLastModifiedTime() throws IOException {
            return 0;
        }

    }

}

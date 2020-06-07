package site.kason.ksh;

import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.common.StreamCopier;
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
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.LocalFileFilter;
import net.schmizz.sshj.xfer.LocalSourceFile;
import net.schmizz.sshj.xfer.TransferListener;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author KasonYang
 */
public class SshClient implements Closeable {

    protected final SSHClient sc = new SSHClient();

    private final List<LocalPortForwardResult> localPfrList = new LinkedList<>();

    private final List<RemotePortForwardResult> remotePfrList = new LinkedList<>();


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
        for (LocalPortForwardResult lpfr : localPfrList) {
            lpfr.close();
        }
        for (RemotePortForwardResult rpfr : remotePfrList) {
            rpfr.close();
        }
        sc.disconnect();
    }

    public void close() throws IOException {
        disconnect();
    }

    public void join() throws TransportException {
        sc.getTransport().join();
    }

    /**
     * set the keep-alive interval
     * @param interval the interval(seconds)
     */
    public void setKeepAliveInterval(int interval) {
        sc.getConnection().getKeepAlive().setKeepAliveInterval(interval);
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
        LocalPortForwardResult pfr = new LocalPortForwardResult(forwardThread);
        localPfrList.add(pfr);
        return pfr;
    }

    public RemotePortForwardResult forwardRemotePort(int remotePort, String host, int port) throws ConnectionException, TransportException {
        RemotePortForwarder forwarder = sc.getRemotePortForwarder();
        SocketForwardingConnectListener sfcl = new SocketForwardingConnectListener(new InetSocketAddress(host, port));
        RemotePortForwarder.Forward forward = new RemotePortForwarder.Forward(remotePort);
        forwarder.bind(forward, sfcl);
        RemotePortForwardResult pfr = new RemotePortForwardResult(forwarder, forward);
        remotePfrList.add(pfr);
        return pfr;
    }

    public void uploadFile(String destPath, String[] localFile,@Nullable FileTransferListener listener) throws IOException {
        File[] files = new File[localFile.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(localFile[i]);
        }
        uploadFile(destPath, files, listener);
    }

    public void uploadFile(String destPath, String... localFile) throws IOException {
        uploadFile(destPath, localFile, null);
    }

    public void uploadFile(String destPath, File[] localFile,@Nullable FileTransferListener listener) throws IOException {
        try (SFTPClient sftp = sc.newSFTPClient()) {
            if (listener != null) {
                sftp.getFileTransfer().setTransferListener(new DefaultTransferListener("", listener));
            }
            for (File lf : localFile) {
                sftp.put(new FileSystemFile(lf), destPath);
            }
        }
    }

    public void uploadFile(String destPath, File... localFile) throws IOException {
        uploadFile(destPath, localFile, null);
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

    public void downloadFile(File localFile, String[] remoteFiles, @Nullable FileTransferListener listener) throws IOException {
        try (SFTPClient sftp = sc.newSFTPClient()) {
            if (listener != null) {
                sftp.getFileTransfer().setTransferListener(new DefaultTransferListener("", listener));
            }
            for (String rf : remoteFiles) {
                sftp.get(rf, new FileSystemFile(localFile));
            }
        }
    }

    public void downloadFile(File localFile, String... remoteFiles) throws IOException {
        downloadFile(localFile, remoteFiles, null);
    }

    public void downloadFile(String localFile, String[] remoteFiles, @Nullable FileTransferListener listener) throws IOException {
        downloadFile(new File(localFile), remoteFiles, listener);
    }

    public void downloadFile(String localFile, String... remoteFiles) throws IOException {
        downloadFile(localFile, remoteFiles, null);
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

    public interface FileTransferListener {

        void transferredSizeChange(String file, long size);

    }

    private static class DefaultTransferListener implements TransferListener {

        private String relativePath;

        private FileTransferListener fileTransferListener;

        private DefaultTransferListener(String relativePath, FileTransferListener fileTransferListener) {
            this.relativePath = relativePath;
            this.fileTransferListener = fileTransferListener;
        }

        @Override
        public TransferListener directory(String name) {
            return new DefaultTransferListener(relativePath + name + "/", fileTransferListener);
        }

        @Override
        public StreamCopier.Listener file(String name, long size) {
            final String path = relativePath + name;
            return transferred -> fileTransferListener.transferredSizeChange(path, size);
        }
    }

}

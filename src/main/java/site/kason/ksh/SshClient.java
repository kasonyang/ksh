package site.kason.ksh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * @author KasonYang
 */
public class SshClient {

    private final SSHClient sc = new SSHClient();


    public void connect(String hostname, int port) throws IOException {
        sc.connect(hostname, port);
    }

    public void addHostKeyVerifier(String fingerprint) {
        sc.addHostKeyVerifier(fingerprint);
    }

    public void disableHostKeyVerify() {
        sc.addHostKeyVerifier(new PromiscuousVerifier());
    }

    public void enableConsoleHostKeyVerify(File khFile) throws IOException {
        sc.addHostKeyVerifier(new ConsoleKnownHostsVerifier(khFile, System.console()));
    }

    public void enableConsoleHostKeyVerify() throws IOException {
        File khFile = new File(OpenSSHKnownHosts.detectSSHDir(), "known_hosts");
        enableConsoleHostKeyVerify(khFile);
    }

    public void authPassword(String username, String password) throws UserAuthException, TransportException {
        sc.authPassword(username, password);
    }

    public void authPublickey(String username, String... locations) throws UserAuthException, TransportException {
        sc.authPublickey(username, locations);
    }

    public void authPublickey(String username) throws UserAuthException, TransportException {
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

    public CommandResult execute(String command) throws ConnectionException, TransportException {
        Session sess = sc.startSession();
        try {
            Session.Command cmd = sess.exec(command);
            cmd.join();
            return new CommandResult(cmd);
        } finally {
            sess.close();
        }
    }

    public void forwardLocalPort(String localHost, int localPort, String remoteHostName, int remotePort) throws IOException {
        Parameters params = new Parameters(localHost, localPort, remoteHostName, remotePort);
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(params.getLocalHost(), params.getLocalPort()));
        LocalPortForwarder forwarder = sc.newLocalPortForwarder(params, ss);
        try {
            forwarder.listen();
        } finally {
            forwarder.close();
            ss.close();
        }
    }

    public void forwardRemotePort(int remotePort, String host, int port) throws ConnectionException, TransportException {
        RemotePortForwarder forwarder = sc.getRemotePortForwarder();
        SocketForwardingConnectListener sfcl = new SocketForwardingConnectListener(new InetSocketAddress(host, port));
        forwarder.bind(new RemotePortForwarder.Forward(remotePort), sfcl);
    }

    public void uploadFile(String destPath, String... localPath) throws IOException {
        try (SFTPClient sftp = sc.newSFTPClient()) {
            for (String lp : localPath) {
                sftp.put(lp, destPath);
            }
        }
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


}

package site.kason.ksh;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Kason Yang
 */
public class Proc {

    private final Process process;

    Proc(Process process) throws IOException {
        this.process = process;
    }

    public OutputStream getOutputStream() {
        return process.getOutputStream();
    }

    public InputStream getInputStream() {
        return process.getInputStream();
    }

    public InputStream getErrorStream() {
        return process.getErrorStream();
    }

    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return process.waitFor(timeout, unit);
    }

    public boolean waitFor(long timeout) throws InterruptedException {
        return waitFor(timeout, TimeUnit.MILLISECONDS);
    }

    public int exitValue() {
        return process.exitValue();
    }

    public boolean exitsWith(int exitValue) {
        return process.exitValue() == exitValue;
    }

    public void destroy() {
        process.destroy();
    }

    public Process destroyForcibly() {
        return process.destroyForcibly();
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public Input input(){
        return new Input(process.getOutputStream());
    }

    public Output output(){
        return new Output(process.getInputStream());
    }

    public Output error(){
        return new Output(process.getErrorStream());
    }

    private String inputStreamToString(InputStream is, String charset) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int rlen;
        while ((rlen = is.read(buffer)) > 0) {
            bos.write(buffer, 0, rlen);
        }
        return bos.toString(charset);
    }

    public static class Input {

        private final OutputStream outputStream;

        private final Charset DEFAULT_CHARSET = Charset.defaultCharset();

        public Input(OutputStream os) {
            this.outputStream = os;
        }

        public Input put(String data) throws IOException {
            return put(data, DEFAULT_CHARSET.name());
        }

        public Input put(String data, String charset) throws IOException {
            outputStream.write(data.getBytes(charset));
            return this;
        }

        public Input put(File file) throws IOException {
            IOUtils.copy(new FileInputStream(file), outputStream);
            return this;
        }

        public void close() throws IOException {
            this.outputStream.close();
        }

    }

    public static class Output {

        private final InputStream inputStream;

        private final Charset DEFAULT_CHARSET = Charset.defaultCharset();

        public Output(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public String string(String charset) throws IOException {
            return IOUtils.toString(inputStream, charset);
        }

        public String string() throws IOException {
            return IOUtils.toString(inputStream, DEFAULT_CHARSET);
        }

    }

}
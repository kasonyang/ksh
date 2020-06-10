package site.kason.ksh;


import org.apache.commons.exec.CommandLine;
import site.kason.ksh.util.StreamUtil;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class Shell {

    private File workingDirectory = new File(".");

    public Shell cd(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public File cd() {
        return workingDirectory;
    }

    public Shell cd(String workingDirectory) {
        return cd(new File(workingDirectory));
    }

    public int exec(String[] arguments) throws IOException, InterruptedException {
        return exec(arguments, "", "", "");
    }

    public int exec(String command) throws IOException, InterruptedException {
        return exec(parseCommandLine(command));
    }

    public int exec(
            String[] arguments, @Nullable String input, @Nullable String output, @Nullable String errOutput
    ) throws IOException, InterruptedException {
        Proc p = start(arguments, input, output, errOutput);
        p.waitFor();
        return p.exitValue();
    }

    public int exec(
            String command, @Nullable String input, @Nullable String output, @Nullable String errOutput
    ) throws IOException, InterruptedException {
        return exec(parseCommandLine(command), input, output, errOutput);
    }

    public Proc start(String[] command) throws IOException {
        return start(command, null, null, null);
    }

    public Proc start(String command) throws IOException {
        return start(parseCommandLine(command));
    }

    public Proc start(
            String[] arguments, @Nullable String input, @Nullable String output, @Nullable String errOutput
    ) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(arguments);
        if (input != null) {
            if (input.isEmpty()) {
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            } else {
                pb.redirectInput(new File(input));
            }
        }
        if (output != null) {
            if (output.isEmpty()) {
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            } else {
                pb.redirectOutput(this.getFileRedirectForOutput(output));
            }
        }
        if (errOutput != null) {
            if (errOutput.isEmpty()) {
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            } else {
                pb.redirectError(this.getFileRedirectForOutput(errOutput));
            }
        }
        pb.directory(workingDirectory);
        return new Proc(pb.start());
    }

    public Proc start(
            String command, @Nullable String input, @Nullable String output, @Nullable String errOutput
    ) throws IOException {
        return start(parseCommandLine(command), input, output, errOutput);
    }

    public CaptureResult capture(String[] arguments, String charset) throws IOException, InterruptedException {
        if (arguments == null || arguments.length == 0) {
            throw new IllegalArgumentException("empty array");
        }
        Process p = Runtime.getRuntime().exec(arguments, null, workingDirectory);
        p.waitFor();
        try (InputStream is = p.getInputStream(); InputStream es = p.getErrorStream()) {
            String output = StreamUtil.readToString(is, charset);
            String error = StreamUtil.readToString(es, charset);
            return new CaptureResult(p.exitValue(), output, error);
        }
    }

    public CaptureResult capture(String command, String charset) throws IOException, InterruptedException {
        return capture(parseCommandLine(command), charset);
    }

    public CaptureResult capture(String[] arguments) throws IOException, InterruptedException {
        return capture(arguments, Charset.defaultCharset().name());
    }

    public CaptureResult capture(String command) throws IOException, InterruptedException {
        return capture(parseCommandLine(command));
    }

    public String captureExec(String[] arguments) throws InterruptedException, IOException {
        return captureExec(arguments, 0);
    }

    public String captureExec(String[] arguments, int expectingExitValue) throws InterruptedException, IOException {
        if (arguments == null || arguments.length == 0) {
            throw new IllegalArgumentException("empty array");
        }
        String command = arguments[0];
        Process p = Runtime.getRuntime().exec(arguments, null, workingDirectory);
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            throw ex;
        }
        int returnValue = p.exitValue();
        if (returnValue != expectingExitValue) {
            String err = String.format("%s exit with a unexpected value %d , expected %d", command, returnValue, expectingExitValue);
            throw new IOException(err);
        }
        p.getOutputStream().close();
        InputStream is = p.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int rlen;
        while ((rlen = is.read(buffer)) > 0) {
            bos.write(buffer, 0, rlen);
        }
        return bos.toString();//TODO using default encoding?
    }

    private String[] parseCommandLine(String commandLine) {
        CommandLine cl = CommandLine.parse(commandLine);
        String[] args = cl.getArguments();
        String[] result = new String[args.length+1];
        result[0] = cl.getExecutable();
        if(args.length>0) {
            System.arraycopy(args,0,result,1, args.length);
        }
        return result;
    }

    private ProcessBuilder.Redirect getFileRedirectForOutput(String redirectFile) {
        if (redirectFile.startsWith(">>")) {
            return ProcessBuilder.Redirect.appendTo(new File(redirectFile.substring(2)));
        } else if (redirectFile.startsWith(">")) {
            return ProcessBuilder.Redirect.to(new File(redirectFile.substring(1)));
        } else {
            return ProcessBuilder.Redirect.appendTo(new File(redirectFile));
        }
    }

    public static class CaptureResult {
        private int exitValue;
        private String output;
        private String error;

        public CaptureResult(int exitValue, String output, String error) {
            this.exitValue = exitValue;
            this.output = output;
            this.error = error;
        }

        public int getExitValue() {
            return exitValue;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }
    }

}
package site.kason.ksh;

import org.apache.commons.cli.*;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CliOptions {

    public static class ParseResult {

        private CommandLine commandLine;

        private String[] args;

        public ParseResult(CommandLine commandLine) {
            this.commandLine = commandLine;
            this.args = commandLine.getArgs();
        }

        public String getOptionValue(String opt,String defaultValue) {
            return commandLine.getOptionValue(opt, defaultValue);
        }

        @Nullable
        public String getOptionValue(String opt) {
            return commandLine.getOptionValue(opt);
        }

        public boolean hasOption(String opt) {
            return commandLine.hasOption(opt);
        }

        public String getArgument(int index,String defaultValue) {
            return index < args.length ? args[index] : defaultValue;
        }

        public String getArgument(int index) {
            return args[index];
        }

        public boolean hasArgument(int index) {
            return index < args.length;
        }

        public String[] getArguments() {
            return commandLine.getArgs();
        }

    }

    private Options options = new Options();

    private int width = 74;

    public CliOptions add(String opt, String description) {
        options.addOption(opt, description);
        return this;
    }

    public CliOptions add(String opt, boolean hasArg, String description) {
        options.addOption(opt, hasArg, description);
        return this;
    }

    public CliOptions add(String opt, String longOpt, boolean hasArg, String description) {
        options.addOption(opt, longOpt, hasArg, description);
        return this;
    }

    public ParseResult parse(String[] args) throws ParseException {
        DefaultParser parser = new DefaultParser();
        return new ParseResult(parser.parse(options, args, true));
    }

    public String format(String usageSyntax,@Nullable String header,@Nullable String footer) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, width, usageSyntax, header, options,1, 3,footer, false);
        return stringWriter.toString();
    }

    public String format(String syntax) {
        return format(syntax,null,null);
    }

}

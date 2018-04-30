package com.imcczy.libds;

/**
 * Created by imcczy.
 */

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.imcczy.libds.reﬁnement.Reﬁnement;
import com.imcczy.libds.utils.Utils;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class CLI {
    private static Options options;

    public static enum OpMode {CLUSTER, MATCH, PARSE, STORE, OTHER}

    public static enum RunMode {SERIAL, PARALLEL, POOL}


    public static class CmdOptions {
        public static Path pathToAndroidJar;
        public static Utils.LOGTYPE logType = Utils.LOGTYPE.CONSOLE;
        public static File logDir = new File("./logs");
        public static Path profilesDir = Paths.get("./profiles");
        public static OpMode opmode = null;
        public static RunMode runMode = RunMode.SERIAL;

    }

    private static List<Path> targetFiles;

    public static void main(String[] args) throws IOException, ClassHierarchyException {
        // parse command line arguments
        parse(args);
        // initialize logback
        initLog();
        LibFactory libFactory = new LibFactory(targetFiles);

        long start = System.nanoTime();
        if (CmdOptions.opmode.equals(OpMode.CLUSTER)) {
            start = System.nanoTime();
            try {
                LibCluster libCluster = new LibCluster(targetFiles);
                libCluster.start2();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (CmdOptions.opmode.equals(OpMode.MATCH)) {
            start = System.nanoTime();
            libFactory.match();
        }

        if (CmdOptions.opmode.equals(OpMode.STORE)) {
            start = System.nanoTime();
            libFactory.store();
        }

        if (CmdOptions.opmode.equals(OpMode.PARSE)) {
            start = System.nanoTime();
            libFactory.parse();
        }
        if (CmdOptions.opmode.equals(OpMode.OTHER)) {
            start = System.nanoTime();
            //Reﬁnement reﬁnement = new Reﬁnement(null);
            //reﬁnement.split();
            libFactory.addsourcename();
        }
        System.out.println(String.format("all task took: %d h",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
    }

    private static void parse(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(setupOptions(), args);

            if (commandLine.hasOption("o")) {
                try {
                    CmdOptions.opmode = OpMode.valueOf(commandLine.getOptionValue("o").toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ParseException(Utils.stacktrace2Str(e));
                }
            } else usage();

            if (checkRequiredUse(commandLine, "a", OpMode.PARSE, OpMode.MATCH, OpMode.STORE, OpMode.OTHER)) {
                CmdOptions.pathToAndroidJar = Paths.get(commandLine.getOptionValue("a"));
            }

            if (checkRequiredUse(commandLine, "p", OpMode.PARSE, OpMode.CLUSTER, OpMode.STORE)) {
                Path profilesDir = Paths.get(commandLine.getOptionValue("p"));
                if (!Files.exists(profilesDir) || !Files.isDirectory(profilesDir))
                    throw new ParseException("Profiles directory " + profilesDir + " doesnt exists and is not a directory");
                CmdOptions.profilesDir = profilesDir;
            }

            if (commandLine.hasOption("d")) {
                CmdOptions.logType = Utils.LOGTYPE.FILE;
                if (commandLine.getOptionValue("d") != null) {
                    File logDir = new File(commandLine.getOptionValue("d"));
                    if (logDir.exists() && !logDir.isDirectory())
                        throw new ParseException("Log directory " + logDir + " already exists and is not a directory");
                    CmdOptions.logDir = logDir;
                }
            }

            if (checkRequiredUse(commandLine, "r", OpMode.PARSE, OpMode.MATCH)) {
                try {
                    CmdOptions.runMode = RunMode.valueOf(commandLine.getOptionValue("r").toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ParseException(Utils.stacktrace2Str(e));
                }
            }


            targetFiles = new ArrayList<Path>();
            String postfix = ".apk";
            if (CmdOptions.opmode.equals(OpMode.CLUSTER)||(CmdOptions.opmode.equals(OpMode.OTHER)))
                postfix = ".lib";
            for (String apkFileName : commandLine.getArgs()) {
                Path arg = Paths.get(apkFileName);
                if (Files.isDirectory(arg)) {
                    targetFiles.addAll(Utils.collectFiles(arg, postfix));
                } else if (Files.exists(arg)) {
                    if (arg.toString().endsWith(postfix))
                        targetFiles.add(arg);
                    else
                        throw new ParseException("File " + arg.getFileName() + " is no valid " + postfix + " file");
                } else {
                    throw new ParseException("Argument is no valid file or directory!");
                }

            }


        } catch (ParseException e) {
            System.err.println("Command line parsing failed:\n" + e.getMessage());
            usage();
        } catch (Exception e) {
            System.err.println("Error occured during argument processing:\n" + e.getMessage());
        }

    }

    private static boolean checkRequiredUse(CommandLine cmd, String option, OpMode... modes) throws ParseException {
        if (!Arrays.asList(modes).contains(CmdOptions.opmode))
            return false;

        if (!cmd.hasOption(option))
            throw new ParseException("Required CLI Option " + option + " is missing in mode " + CmdOptions.opmode);

        return true;
    }

    private static Options setupOptions() {
        options = new Options();


        options.addOption(Option.builder("o")
                .argName("value")
                .required(true)
                .longOpt("opmode")
                .desc("mode of operation, one of [PARSE|CLUSTER|MATCH]")
                .hasArg()
                .build());

        options.addOption(Option.builder("a")
                .argName("file")
                .required(true)
                .longOpt("android")
                .desc("path to android.jar")
                .hasArg()
                .build());

        options.addOption(Option.builder("d")
                .argName("directory")
                .required(false)
                .longOpt("log-dir")
                .desc("path to store the log")
                .hasArg()
                .build());

        options.addOption(Option.builder("p")
                .argName("directory")
                .required(false)
                .longOpt("profiles-dir")
                .desc("path to store the app profile")
                .hasArg()
                .build());

        options.addOption(Option.builder("r")
                .argName("value")
                .required(false)
                .longOpt("runmode")
                .desc("the run mode default serial(or parallel)")
                .hasArg()
                .build());

        return options;
    }

    private static void initLog() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator joranConfigurator = new JoranConfigurator();
            joranConfigurator.setContext(loggerContext);
            loggerContext.reset();
            joranConfigurator.doConfigure("./logback.xml");
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            switch (CmdOptions.logType) {
                case CONSOLE:
                    rootLogger.detachAppender("FILE");
                    break;
                case FILE:
                    rootLogger.detachAppender("CONSOLE");
                    break;
                case NONE:
                    rootLogger.detachAndStopAllAppenders();
                    break;
            }
        } catch (JoranException e) {

        }

        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    private static final String TOOLNAME = "libds";
    private static final String USAGE = TOOLNAME + " --opmode [PARSE|MATCH|CLUSTER|STORE] <options>";
    private static final String USAGE_PROFILE = TOOLNAME + " --opmode parse -a <path-to-android.jar> -p <path-to-profiles> -d <path-to-log> -r serial path-to-apk";
    private static final String USAGE_MATCH = TOOLNAME + " --opmode match -a <path-to-android.jar> -p <path-to-profiles> -d <path-to-log> -r serial path-to-apk";
    private static final String USAGE_DB = TOOLNAME + " --opmode cluster -a <path-to-android.jar> -p <path-to-profiles> -d <path-to-log> -r serial path-to-apk";

    private static void usage() {
        HelpFormatter formatter = new HelpFormatter();
        String helpMsg = USAGE;
        if (OpMode.PARSE.equals(CmdOptions.opmode))
            helpMsg = USAGE_PROFILE;
        else if (OpMode.MATCH.equals(CmdOptions.opmode))
            helpMsg = USAGE_MATCH;
        else if (OpMode.CLUSTER.equals(CmdOptions.opmode))
            helpMsg = USAGE_DB;

        formatter.printHelp(helpMsg, options);
        System.exit(1);
    }
}
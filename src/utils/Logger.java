package utils;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import agents.Company;
import types.Finance;
import types.Record;

public class Logger {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private static final boolean USE_COLOURS = true;

    private static final String LOG_FOLDER = "./log/";
    private static final Path COMPANIES_AGGREGATE = Paths.get(LOG_FOLDER + "COMPANIES");
    private static final Path STATIONS_AGGREGATE = Paths.get(LOG_FOLDER + "STATIONS");
    private static final Path CLIENTS_AGGREGATE = Paths.get(LOG_FOLDER + "CLIENTS");

    private static final boolean SHOW_CLIENT = false;
    private static final boolean SHOW_STATION = false;
    private static final boolean SHOW_COMPANY = true;
    private static final boolean SHOW_GOD = true;
    public static final boolean RECORD_DEBUG = true;
    public static final boolean RECORD_COMPANY_VERBOSE = true;
    public static final boolean RECORD_STATION_VERBOSE = true;
    public static final boolean RECORD_CLIENT_VERBOSE = true;

    public enum Format { CSV, TABLE }
    public static final Format AGGREGATE_FORMAT = Format.CSV;
    public static final Format COMPANY_FORMAT = Format.TABLE;

    // * Generic

    public static void clearLogFolder() {
        File dir = new File(LOG_FOLDER);
        for (File file : dir.listFiles()) file.delete();
    }

    public static void console(String pre, String name, String text) {
        if (USE_COLOURS) {
            System.out.println(pre + "[" + name + "]:" + ANSI_RESET + " " + text);
        } else {
            System.out.println("[" + name + "]: " + text);
        }
    }

    public static void single(String id, String text) {
        Path path = Paths.get(LOG_FOLDER + id);
        try {
            Files.write(path, text.getBytes(), APPEND, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void aggregateHeaders() {
        // Company
        String headerCompany = Company.CompanyRecord.header(AGGREGATE_FORMAT);
        String headerFinance = Finance.header(AGGREGATE_FORMAT);
        String header = Record.line(AGGREGATE_FORMAT, headerCompany, headerFinance);
        aggregate(COMPANIES_AGGREGATE, header);
    }

    public static void aggregate(Path path, String text) {
        try {
            Files.write(path, text.getBytes(), APPEND, CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendCompany(String text) {
        aggregate(COMPANIES_AGGREGATE, text);
    }

    public static void appendStation(String text) {
        aggregate(STATIONS_AGGREGATE, text);
    }

    public static void appendClient(String text) {
        aggregate(CLIENTS_AGGREGATE, text);
    }

    // * colors

    public static void red(String agentName, String text) {
        console(ANSI_RED, agentName, text);
    }

    public static void green(String agentName, String text) {
        console(ANSI_GREEN, agentName, text);
    }

    public static void yellow(String agentName, String text) {
        console(ANSI_YELLOW, agentName, text);
    }

    public static void blue(String agentName, String text) {
        console(ANSI_BLUE, agentName, text);
    }

    public static void purple(String agentName, String text) {
        console(ANSI_PURPLE, agentName, text);
    }

    public static void cyan(String agentName, String text) {
        console(ANSI_CYAN, agentName, text);
    }

    public static void white(String agentName, String text) {
        console(ANSI_WHITE, agentName, text);
    }

    // * single logging

    public static void client(String agentName, String text) {
        if (!SHOW_CLIENT) return;
        console(ANSI_WHITE, agentName, text);
    }

    public static void station(String agentName, String text) {
        if (!SHOW_STATION) return;
        console(ANSI_RED, agentName, text);
    }

    public static void company(String agentName, String text) {
        if (!SHOW_COMPANY) return;
        console(ANSI_PURPLE, agentName, text);
    }

    public static void god(String text) {
        if (!SHOW_GOD) return;
        console(ANSI_YELLOW, "god", text);
    }
}

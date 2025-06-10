package app.model.reflection;

import app.utility.DateTextFormatter;
import google.api.SheetService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReflectionWriter {
    private static final Executor executor = Executors.newSingleThreadExecutor();

    private enum WriteDestination {
        GOOGLE_SHEETS,
        TEXT_FILE,
        BINARY_FILE
    }

    private static Executor getExecutor() {
        return executor;
    }

    private static final int LOGGING_START_HOUR = 9;
    private static final int LOGGING_END_HOUR = 24;
    private static final String TIME_PERIOD_DAY_OVERALL = "за день";


    private static final String SHEETS_DATE_COLUMN_RANGE = "A:A";

    private static final String FAILURES_FOLDER = "data/failures/";

    private final static List<Object> SHEETS_PERIOD_ROW_MARKUP = new ArrayList<>() {{
        add(TIME_PERIOD_DAY_OVERALL);
        for (int hour = LOGGING_START_HOUR; hour <= LOGGING_END_HOUR; hour++) {
            add(DateTextFormatter.getTimePeriod(hour % 24));
        }
    }};

    private final static List<Object> SHEETS_ACTIVITY_ROW_MARKUP = Arrays.asList("", "занятие");
    private final static List<Object> SHEETS_PLEASURE_ROW_MARKUP = Arrays.asList("", "удовольствие");
    private final static List<Object> SHEETS_VALUE_ROW_MARKUP = Arrays.asList("", "ценность");

    private final Reflection reflection;
    private final WriteDestination writeDestination;
    private final String filePath;

    private boolean isFileDestination(WriteDestination writeDestination) {
        return List.of(WriteDestination.BINARY_FILE, WriteDestination.TEXT_FILE)
                .contains(writeDestination);
    }

    private ReflectionWriter(Builder builder) throws IllegalArgumentException{
        this.reflection = builder.reflection;
        this.writeDestination = builder.writeDestination;
        this.filePath = builder.filePath;

        if (isFileDestination(writeDestination) && filePath.isBlank()) {
            throw new IllegalArgumentException("[Reflection Writer] File path required for this write type");
        }
    }

    public static int appendNewReflectionDay(LocalDateTime dateTime) {
        try {
            List<Object> mainRowMarkup = new ArrayList<>(Arrays.asList(
                    DateTextFormatter.getDate(dateTime),
                    DateTextFormatter.getWeekDay(dateTime)
            ));
            mainRowMarkup.addAll(SHEETS_PERIOD_ROW_MARKUP);

            int newRowIdx = SheetService.appendRow(mainRowMarkup);
            SheetService.appendRow(SHEETS_ACTIVITY_ROW_MARKUP);
            SheetService.appendRow(SHEETS_PLEASURE_ROW_MARKUP);
            SheetService.appendRow(SHEETS_VALUE_ROW_MARKUP);

            return newRowIdx;
        } catch (IOException ioex) {
            System.out.println("[Reflection New Row] Couldn't append. " + ioex.getMessage());
            return -1;
        }
    }

    public void write() {
        Runnable writeTask = switch (writeDestination) {
            case GOOGLE_SHEETS -> this::writeGoogleSheets;
            case BINARY_FILE -> this::writeObjectFile;
            case TEXT_FILE -> this::writeTextFile;
        };

        getExecutor().execute(writeTask);
    }

    public static class Builder {
        private final Reflection reflection;
        private WriteDestination writeDestination = WriteDestination.GOOGLE_SHEETS;
        private String filePath = "";

        public Builder(Reflection reflection) {
            this.reflection = reflection;
        }

        public Builder sheets() {
            this.writeDestination = WriteDestination.GOOGLE_SHEETS;
            return this;
        }

        public Builder toPath(String path) {
            this.filePath = path;
            return this;
        }

        public Builder binary() {
            this.writeDestination = WriteDestination.BINARY_FILE;
            return this;
        }

        public Builder text() {
            this.writeDestination = WriteDestination.TEXT_FILE;
            return this;
        }

        public ReflectionWriter build() {
            return new ReflectionWriter(this);
        }
    }

    private void writeGoogleSheets() {
        try {
            int entryRowIndex = SheetService.getRowIdxForEntry(SHEETS_DATE_COLUMN_RANGE, DateTextFormatter.getDate(reflection.getDateTime()));

            if (entryRowIndex < 0) {
                entryRowIndex = appendNewReflectionDay(reflection.getDateTime());
            }

            if (entryRowIndex < 0) {
                throw new NullPointerException("[Save Reflection] Couldn't append new row");
            }

            int periodEntryColumnIdx = SheetService.getColumnIdxForRowEntry(entryRowIndex, reflection.getTargetTimePeriod());

            if (periodEntryColumnIdx < 0) {
                throw new NullPointerException("[Save Reflection] Couldn't find necessary time period");
            }

            SheetService.updateCellValue(entryRowIndex + 1, periodEntryColumnIdx, reflection.getActivity());
            SheetService.updateCellValue(entryRowIndex + 2, periodEntryColumnIdx, reflection.getPleasure());
            SheetService.updateCellValue(entryRowIndex + 3, periodEntryColumnIdx, reflection.getValue());
        } catch (NullPointerException | IOException ex) {
            System.out.printf("[Save Reflection] Google API Sheets fail. %s\n", ex.getMessage());
            ReflectionWriter failureWriter =
                    new Builder(reflection)
                            .text()
                            .toPath(FAILURES_FOLDER)
                            .build();

            failureWriter.write();
        }
    }

    private void writeTextFile() {
        try {
            String fileName = hashCode() + ".txt";
            File reflectionFile = new File(filePath);
            if (reflectionFile.mkdirs()) {
                System.out.println("[Write Reflection File] Directory " + reflectionFile.getPath() + " successfully created");
            }

            LocalDateTime reflectionTime = reflection.getDateTime();
            FileWriter fileWriter = new FileWriter(Paths.get(reflectionFile.getPath(), fileName).toString());
            fileWriter.write("targetDate=" + DateTextFormatter.getDate(reflectionTime) + "\n");
            fileWriter.write("targetTimePeriod=" + reflection.getTargetTimePeriod() + "\n");
            fileWriter.write("timestamp=" + reflection.getTimestamp() + "\n");
            fileWriter.write("weekDay=" + DateTextFormatter.getWeekDay(reflectionTime) + "\n");
            fileWriter.write("activity=" + reflection.getActivity() + "\n");
            fileWriter.write("pleasure=" + reflection.getPleasure() + "\n");
            fileWriter.write("value=" + reflection.getValue() + "\n");
            fileWriter.close();
        } catch (IOException ioex) {
            System.out.printf("[Write Reflection File] Couldn't save to the disk: %s\n", ioex.getMessage());
        }
    }

    private void writeObjectFile() {

    }

    public static int getLoggingStartHour() {
        return LOGGING_START_HOUR;
    }

    public static int getLoggingEndHour() {
        return LOGGING_END_HOUR;
    }

    public static String getOverallTimePeriod() {
        return TIME_PERIOD_DAY_OVERALL;
    }

    public static String buildMessage(Reflection reflection, String message) {
        return String.format("[%s] \n%s", print(reflection), message);
    }

    public static String print(Reflection reflection) {
        LocalDateTime reflectionTime = reflection.getDateTime();
        return "${DATE} ${PERIOD} (${WEEKDAY})"
                .replace("${DATE}", DateTextFormatter.getDate(reflectionTime))
                .replace("${PERIOD}", reflection.getTargetTimePeriod())
                .replace("${WEEKDAY}", DateTextFormatter.getWeekDay(reflectionTime));
    }
}

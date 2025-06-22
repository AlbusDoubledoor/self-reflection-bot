package app;

import app.model.reflection.PollReflectionQueue;
import app.model.reflection.Reflection;
import app.model.reflection.ReflectionWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import telegram.bot.SelfReflectionBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SelfReflectionBotApp {
    private static final Logger log = LogManager.getLogger(SelfReflectionBotApp.class);
    private final static String TELEGRAM_BOT_API_TOKEN = System.getenv("telegram_bot_token");
    private final static String TARGET_USER_ID = System.getenv("telegram_bot_target_user");
    private static final String APP_MODE = System.getenv("app_mode");

    private static final String APP_MODES_DEBUG = "DEBUG";
    private static final String BOT_TIMEZONE = "CET";

    private static final int BOT_POLL_INITIAL_DELAY = 0;
    private static final int BOT_POLL_INTERVAL = 5;
    private static final TimeUnit BOT_POLL_UNIT = TimeUnit.MINUTES;
    private static final int BOT_POLL_MINUTE_START = 1;
    private static final int BOT_POLL_MINUTE_END = 15;

    private static final int BOT_POLL_CLEANUP_HOUR = 3;

    private static final long BOT_POLL_SAVE_QUEUE_INITIAL_DELAY = 1;
    private static final long BOT_POLL_SAVE_QUEUE_INTERVAL = 1;
    private static final TimeUnit BOT_POLL_SAVE_QUEUE_UNIT = TimeUnit.HOURS;

    private static final String DATA_QUEUE_DAT = "data/poll_queue.ser";

    private static final int BOT_POLL_PREPARE_NEW_INITIAL_DELAY = 1;
    private static final int BOT_POLL_PREPARE_NEW_INTERVAL = 1;
    private static final TimeUnit BOT_POLL_PREPARE_NEW_UNIT = TimeUnit.HOURS;

    private final ScheduledExecutorService botExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Set<Integer> hourlyRequestRegister = new HashSet<>();
    private SelfReflectionBot selfReflectionBot;

    public void start() throws TelegramApiException {
        log.info("Starting Self-Reflection Bot application with user={} and mode={}", TARGET_USER_ID, APP_MODE);

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        String sTargetUserId = TARGET_USER_ID;
        long targetUserId = -1;

        try {
            targetUserId = Long.parseLong(sTargetUserId);
        } catch (NumberFormatException nfe) {
            log.error("Couldn't parse target user ID <{}>", sTargetUserId);
            return;
        }

        selfReflectionBot = new SelfReflectionBot(TELEGRAM_BOT_API_TOKEN, targetUserId);

        PollReflectionQueue pollReflectionQueue;

        try {
            pollReflectionQueue = PollReflectionQueue.loadFromFile(DATA_QUEUE_DAT);
            log.info("Poll queue successfully loaded from file {}", DATA_QUEUE_DAT);
        } catch (Exception e) {
            pollReflectionQueue = new PollReflectionQueue();
            log.info("Starting with a fresh poll queue, file {} created", DATA_QUEUE_DAT);
        }

        selfReflectionBot.linkPollQueue(pollReflectionQueue);

        if (APP_MODE.equals(APP_MODES_DEBUG)) {
            selfReflectionBot.enableDebugMode(true);

            log.debug("Debug mode activated");
        }

        botsApi.registerBot(selfReflectionBot);

        botExecutor.scheduleAtFixedRate(this::pollActivityTask, BOT_POLL_INITIAL_DELAY, BOT_POLL_INTERVAL, BOT_POLL_UNIT);
        botExecutor.scheduleAtFixedRate(this::savePollQueue, BOT_POLL_SAVE_QUEUE_INITIAL_DELAY, BOT_POLL_SAVE_QUEUE_INTERVAL, BOT_POLL_SAVE_QUEUE_UNIT);
        botExecutor.scheduleAtFixedRate(this::prepareNewDay, BOT_POLL_PREPARE_NEW_INITIAL_DELAY, BOT_POLL_PREPARE_NEW_INTERVAL, BOT_POLL_PREPARE_NEW_UNIT);

        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
    }

    private void pollActivityTask() {
        LocalDateTime now = LocalDateTime.now(getTimezoneId());
        int hour = now.getHour();
        int minute = now.getMinute();

        int finalHour = (ReflectionWriter.getLoggingEndHour() % 24);

        if (hour >= ReflectionWriter.getLoggingStartHour() || hour == finalHour) {
            if (minute >= BOT_POLL_MINUTE_START && minute <= BOT_POLL_MINUTE_END) {
                if (!hourlyRequestRegister.contains(hour)) {
                    log.info("Sending new hourly request");
                    selfReflectionBot.addPoll(new Reflection(now));

                    if (hour == finalHour) {
                        selfReflectionBot.addPoll(new Reflection(now, ReflectionWriter.getOverallTimePeriod()));
                    }

                    hourlyRequestRegister.add(hour);
                }
            }
        }
    }

    private void prepareNewDay() {
        LocalDateTime now = LocalDateTime.now(getTimezoneId());
        int hour = now.getHour();

        if (hour == BOT_POLL_CLEANUP_HOUR) {
            hourlyRequestRegister.clear();

            appendNewDay();
            log.info("New day appended");

            PollReflectionQueue pollReflectionQueue = selfReflectionBot.getPollReflectionQueue();
            try {
                int initialSize = pollReflectionQueue.size();
                pollReflectionQueue.cleanupOld();
                int resultSize =  pollReflectionQueue.size();
                log.info("Clean-up of request queue completed. Old size <{}>; new size <{}>", initialSize, resultSize);
            } catch (NullPointerException npe) {
                log.error("Couldn't clean old polls because queue was <null>");
            }
        }
    }

    private void savePollQueue() {
        try {
            PollReflectionQueue pollReflectionQueue = selfReflectionBot.getPollReflectionQueue();
            pollReflectionQueue.saveToFile(DATA_QUEUE_DAT);
            log.info("Request queue successfully saved. Size <{}>", pollReflectionQueue.size());
        } catch (IOException ioex) {
            log.error("Couldn't save queue file due to an error: {}", ioex.getMessage());
        }
    }

    private void onShutdown() {
        savePollQueue();
    }

    public ZoneId getTimezoneId() {
        return ZoneId.of(BOT_TIMEZONE);
    }

    public void appendNewDay() {
        LocalDateTime dateTime = LocalDateTime.now(getTimezoneId());
        ReflectionWriter.appendNewReflectionDay(dateTime);
    }

    public SelfReflectionBot getSelfReflectionBot() {
        return selfReflectionBot;
    }

    public static void main(String[] args) {
        SelfReflectionBotApp botApp = new SelfReflectionBotApp();

        try {
            botApp.start();
        } catch (TelegramApiException tapiex) {
            log.error("Unable to start bot app due to an exception: {}", tapiex.getMessage());
        }
    }
}

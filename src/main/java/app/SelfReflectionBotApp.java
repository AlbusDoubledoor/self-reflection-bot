package app;

import app.model.reflection.PollReflectionQueue;
import app.model.reflection.Reflection;
import app.model.reflection.ReflectionWriter;
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

    private static final String DATA_QUEUE_DAT = "data/queue.dat";

    private static final int BOT_POLL_PREPARE_NEW_INITIAL_DELAY = 1;
    private static final int BOT_POLL_PREPARE_NEW_INTERVAL = 1;
    private static final TimeUnit BOT_POLL_PREPARE_NEW_UNIT = TimeUnit.HOURS;

    private final ScheduledExecutorService botExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Set<Integer> hourlyRequestRegister = new HashSet<>();
    private SelfReflectionBot selfReflectionBot;

    public void start() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        String sTargetUserId = TARGET_USER_ID;
        long targetUserId = -1;

        try {
            targetUserId = Long.parseLong(sTargetUserId);
        } catch (NumberFormatException nfe) {
            System.out.printf("Couldn't parse target user ID <%s>\n", sTargetUserId);
            return;
        }

        selfReflectionBot = new SelfReflectionBot(TELEGRAM_BOT_API_TOKEN, targetUserId);

        PollReflectionQueue pollReflectionQueue;

        try {
            pollReflectionQueue = PollReflectionQueue.loadFromFile(DATA_QUEUE_DAT);
            System.out.println("[Self Reflection Bot App] Loaded queue from file");
        } catch (Exception e) {
            pollReflectionQueue = new PollReflectionQueue();
            System.out.println("[Self Reflection Bot App] Starting with a fresh queue");
        }

        selfReflectionBot.linkPollQueue(pollReflectionQueue);

        if (APP_MODE.equals(APP_MODES_DEBUG)) {
            selfReflectionBot.enableDebugMode(true);
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

            PollReflectionQueue pollReflectionQueue = selfReflectionBot.getPollReflectionQueue();
            try {
                pollReflectionQueue.cleanupOld();
            } catch (NullPointerException npe) {
                System.out.println("[Self Reflection Bot App] Couldn't clean old polls because queue was <null>");
            }
        }
    }

    private void savePollQueue() {
        try {
            PollReflectionQueue pollReflectionQueue = selfReflectionBot.getPollReflectionQueue();
            pollReflectionQueue.saveToFile(DATA_QUEUE_DAT);
            System.out.println("[Self Reflection Bot App] Queue successfully saved");
        } catch (IOException ioex) {
            System.out.println("[Self Reflection Bot App] Couldn't save queue file due to error: " + ioex.getMessage());
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
            System.out.println("Unable to start bot app due to an exception: " + tapiex.getMessage());
        }
    }
}

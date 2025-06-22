package com.kanashi.srb.telegram.bot;

import com.kanashi.srb.app.flow.BotFlow;
import com.kanashi.srb.app.flow.PollActivityFlow;
import com.kanashi.srb.app.model.reflection.PollReflectionQueue;
import com.kanashi.srb.app.model.reflection.Reflection;
import com.kanashi.srb.app.model.reflection.ReflectionWriter;
import com.kanashi.srb.app.utility.DateTextFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import com.kanashi.srb.telegram.bot.model.ext.CallbackQueryExt;
import com.kanashi.srb.telegram.bot.model.menu.QuestionMenu;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.random.RandomGenerator;

public class SelfReflectionBot extends BasicBot {
    private static final Logger log = LogManager.getLogger(SelfReflectionBot.class);
    private static final String BOT_USERNAME = "SelfReflectionBot";
    private long targetUserId = 0L;
    private BotFlow currentFlow;
    private PollReflectionQueue pollReflectionQueue;
    private boolean debugMode = false;

    private enum UpdateType {
        MESSAGE("Message"),
        CALLBACK_QUERY("Callback Query"),
        UNKNOWN("Unknown");


        private final String displayName;

        UpdateType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final String MSG_POLL_QUEUE_NULL = "[Bot: Add Poll] Poll Queue is not associated";

    private static final String MESSAGE__OFFER_RATING = "Время оценить своё поведение. Будем оценивать?";
    private static final String MESSAGE__REQUEST_NOT_FOUND = "[Удалено, так как запрос не найден. Период хранения запросов - 7 дней]";
    private static final String MESSAGE__REQUEST_ACCEPTED = "Запрос на запись принят \u2705";
    private static final String MESSAGE__REQUEST_SKIPPED = "Запрос на запись пропущен \u274C";

    public SelfReflectionBot(String botToken, long targetUserId) {
        super(botToken, BOT_USERNAME);
        this.targetUserId = targetUserId;
    }

    public void linkPollQueue(PollReflectionQueue pollReflectionQueue) {
        this.pollReflectionQueue = pollReflectionQueue;
    }

    @Override
    public void onUpdateReceived(Update update) {
        long userId = targetUserId;
        UpdateType updateType = UpdateType.UNKNOWN;

        if (update.hasMessage()) {
            userId = update.getMessage().getFrom().getId();
            updateType = UpdateType.MESSAGE;
        } else if (update.hasCallbackQuery()) {
            userId = update.getCallbackQuery().getFrom().getId();
            updateType = UpdateType.CALLBACK_QUERY;
        }

        if (userId != targetUserId) {
            log.warn("Received {} from unknown user id={}", updateType, userId);
            return;
        }

        if (updateType.equals(UpdateType.UNKNOWN)) {
            log.warn("Received unknown operation: {}", update);
        }

        dispatchPoll(update);
        dispatchFlow(update);
        dispatchCommand(update);
    }

    public void enableDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    private void simulatePoll() {
        Reflection reflection = new Reflection(LocalDateTime.now());
        addPoll(reflection);
    }

    private void simulateRandomPoll() {
        int hour = RandomGenerator.getDefault().nextInt(10) + 9;
        Reflection reflection = new Reflection(LocalDateTime.now(), DateTextFormatter.getTimePeriod(hour));
        addPoll(reflection);
    }

    private void simulateDayPoll() {
        Reflection reflection = new Reflection(LocalDateTime.now(), ReflectionWriter.getOverallTimePeriod());
        addPoll(reflection);
    }

    private void printQueue() {
        log.debug(pollReflectionQueue.toString());
    }

    public void addPoll(Reflection reflection) throws NullPointerException {
        if (pollReflectionQueue == null) {
            throw new NullPointerException(MSG_POLL_QUEUE_NULL);
        }
        log.debug("New poll added to the queue; timeperiod={} / id={}", reflection.getTargetTimePeriod(), reflection.getId());
        sendMenu(ReflectionWriter.buildMessage(reflection, MESSAGE__OFFER_RATING), new QuestionMenu(reflection.getId()).getKeyboard());
        pollReflectionQueue.add(reflection);
    }

    public PollReflectionQueue getPollReflectionQueue() {
        return pollReflectionQueue;
    }

    public void dispatchPoll(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQueryExt callbackQueryExt = CallbackQueryExt.extend(update.getCallbackQuery());

            if (!callbackQueryExt.hasPurpose(QuestionMenu.getPurpose())) {
                return;
            }

            // Handle menu callback - edit message and start flow
            String answer = callbackQueryExt.getCallbackPayload();
            String reflectionId = callbackQueryExt.getCallbackId();
            String newMessageText = "";

            Reflection targetReflection = pollReflectionQueue.getById(reflectionId);

            if (targetReflection != null) {
                if (answer.equals(QuestionMenu.getAnswerPositive())) {
                    // If answer is YES - create new flow
                    newMessageText = ReflectionWriter.buildMessage(targetReflection, MESSAGE__REQUEST_ACCEPTED);
                    if (currentFlow != null) {
                        currentFlow.end();
                    }
                    currentFlow = new PollActivityFlow(this ,targetReflection);
                    currentFlow.start();
                } else {
                    // If answer is NO - reflection skipped
                    newMessageText = ReflectionWriter.buildMessage(targetReflection, MESSAGE__REQUEST_SKIPPED);
                }

                pollReflectionQueue.removeById(reflectionId);
            } else {
                newMessageText = MESSAGE__REQUEST_NOT_FOUND;
            }

            editMessageCallback(callbackQueryExt.getOriginal(), newMessageText);

            // Wrap up callback handler
            answerCallback(callbackQueryExt.getOriginal());
        }
    }
    public void dispatchCommand(Update update) {
        if (update.hasMessage()) {
            Message msg = update.getMessage();

            if (msg.isCommand()) {
                String txt = "";
                if (msg.hasText()) {
                    txt = msg.getText();
                    if (debugMode) {
                        try {
                            log.debug("Executing command-method = {}", txt);
                            Method method = this.getClass().getDeclaredMethod(txt.replace("/", ""));
                            method.invoke(this);
                        } catch (Exception e) {
                            log.warn("Command-method invoke exception = {}: {}", txt, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void dispatchFlow(Update update) {
        if (currentFlow != null) {
            if (currentFlow.isFinished()) {
                currentFlow = null;
                return;
            }

            currentFlow.handleUpdate(update);
        }
    }

    public void sendText(String text) {
        super.sendText(this.targetUserId, text);

    }

    public void sendMenu(String text, InlineKeyboardMarkup keyboard) {
        super.sendMenu(this.targetUserId, text, keyboard);
    }
}

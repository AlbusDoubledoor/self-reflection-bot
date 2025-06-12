package telegram.bot;

import app.SelfReflectionBotApp;
import app.flow.BotFlow;
import app.flow.PollActivityFlow;
import app.model.reflection.PollReflectionQueue;
import app.model.reflection.Reflection;
import app.model.reflection.ReflectionWriter;
import app.utility.DateTextFormatter;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import telegram.bot.model.ext.CallbackQueryExt;
import telegram.bot.model.menu.QuestionMenu;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.random.RandomGenerator;

public class SelfReflectionBot extends BasicBot {
    private final static String BOT_USERNAME = "SelfReflectionBot";
    private long targetUserId = 0L;
    private BotFlow currentFlow;
    private PollReflectionQueue pollReflectionQueue;
    private boolean debugMode = false;

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
        if (update.hasMessage() && update.getMessage().getFrom().getId() != targetUserId) {
            return;
        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom().getId() != targetUserId) {
            return;
        }

        dispatchPoll(update);
        dispatchFlow(update);
        dispatchCommand(update);
    }

    public void enableDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    public void simulatePoll() {
        Reflection reflection = new Reflection(LocalDateTime.now());
        addPoll(reflection);
    }

    public void simulateRandomPoll() {
        Reflection reflection = new Reflection(LocalDateTime.now());
        int hour = RandomGenerator.getDefault().nextInt(10) + 9;
        reflection.setTargetTimePeriod(DateTextFormatter.getTimePeriod(hour));
        addPoll(reflection);
    }

    public void simulateDayPoll() {
        Reflection reflection = new Reflection(LocalDateTime.now());
        reflection.setTargetTimePeriod(ReflectionWriter.getOverallTimePeriod());
        addPoll(reflection);
    }

    public void addPoll(Reflection reflection) throws  NullPointerException {
        if (pollReflectionQueue == null) {
            throw new NullPointerException("[Bot: Add Poll] Poll Queue is not associated");
        }
        sendMenu(ReflectionWriter.buildMessage(reflection, MESSAGE__OFFER_RATING), QuestionMenu.getKeyboard(reflection.getId()));
        pollReflectionQueue.add(reflection);
    }

    public PollReflectionQueue getPollReflectionQueue() {
        return pollReflectionQueue;
    }

    public void dispatchPoll(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQueryExt callbackQueryExt = CallbackQueryExt.extend(update.getCallbackQuery());

            if (!callbackQueryExt.ofPurpose(QuestionMenu.getPurpose())) {
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
                            Method method = this.getClass().getDeclaredMethod(txt.replace("/", ""));
                            method.invoke(this);
                        } catch (Exception e) {
                            System.out.printf("Command-method invoke exception %s\n", e.getMessage());
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

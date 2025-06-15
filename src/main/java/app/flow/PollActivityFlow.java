package app.flow;

import app.model.reflection.ReflectionWriter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import telegram.bot.SelfReflectionBot;
import telegram.bot.model.ext.CallbackQueryExt;
import telegram.bot.model.menu.ScaledRateMenu;
import org.telegram.telegrambots.meta.api.objects.Update;
import app.model.reflection.Reflection;


public class PollActivityFlow implements BotFlow {
    private static final String FLOW_NAME = "LogReflection";

    private enum STAGE {
        ACTIVITY,
        ESTIMATE_PLEASURE,
        ESTIMATE_VALUE,
        CONCLUSION
    }

    private STAGE currentStage;
    private final SelfReflectionBot selfReflectionBot;
    private final Reflection reflection;
    private boolean isFinished = false;

    /* Messages */
    private static final String MESSAGE_UTILITY__RATE_TEMPLATE = "${RATE}";

    private static final String MESSAGE__ACTIVITY_NAME = "Как записать занятие в этот период?";
    private static final String MESSAGE__RATE_PLEASURE = "Оцените удовольствие";
    private static final String MESSAGE__RATE_VALUE = "Оцените ценность";
    private static final String MESSAGE__SHOW_PLEASURE = "Оценка удовольствия: " + MESSAGE_UTILITY__RATE_TEMPLATE;
    private static final String MESSAGE__SHOW_VALUE = "Оценка ценности: " + MESSAGE_UTILITY__RATE_TEMPLATE;
    private static final String MESSAGE__SAVE_CONFIRMATION_FORMAT = """
            *** Супер, данные записаны ***\s
            <b>Занятие</b>: %s\s
            <b>Удовольствие</b>: %s\s
            <b>Ценность</b>: %s""";
    private final InlineKeyboardMarkup scaledRateKeyboard = new ScaledRateMenu().getKeyboard();

    public PollActivityFlow(SelfReflectionBot selfReflectionBot, Reflection reflection) {
        this.selfReflectionBot = selfReflectionBot;
        this.reflection = reflection;

    }

    private String buildMessage(String message) {
        return ReflectionWriter.buildMessage(reflection, message);
    }

    @Override
    public void start() {
        currentStage = STAGE.ACTIVITY;
    }

    @Override
    public void move() {
        switch (currentStage) {
            case STAGE.ACTIVITY -> currentStage = STAGE.ESTIMATE_PLEASURE;
            case STAGE.ESTIMATE_PLEASURE -> currentStage = STAGE.ESTIMATE_VALUE;
            case STAGE.ESTIMATE_VALUE -> currentStage = STAGE.CONCLUSION;
        }
    }

    @Override
    public void end() {
        isFinished = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    public void refresh() {
        handleUpdate(null);
    }

    @Override
    public void handleUpdate(Update update) {
        if (isFinished) {
            return;
        }

        switch (currentStage) {
            /*
             * Stage #1 - Ask if user ready to rate their activity
             */
            case STAGE.ACTIVITY -> {
                selfReflectionBot.sendText(buildMessage(MESSAGE__ACTIVITY_NAME));
                move();
            }

            /*
             * Stage #2 - Record activity name and ask to rate pleasure
             */
            case STAGE.ESTIMATE_PLEASURE -> {
                if (update.hasMessage() && update.getMessage().hasText()) {
                    String txt = update.getMessage().getText();
                    reflection.setActivity(txt);
                    selfReflectionBot.sendMenu(buildMessage(MESSAGE__RATE_PLEASURE), scaledRateKeyboard);
                    move();
                }
            }

            /*
             * Stage #3 - Record pleasure and ask to rate value
             */
            case STAGE.ESTIMATE_VALUE -> {
                if (update.hasCallbackQuery()) {
                    CallbackQueryExt callbackQueryExt = CallbackQueryExt.extend(update.getCallbackQuery());

                    if (!callbackQueryExt.hasPurpose(ScaledRateMenu.getPurpose())) {
                        break;
                    }

                    // Handle menu callback - edit message and set rate
                    String rate = ScaledRateMenu.getRate(callbackQueryExt);
                    selfReflectionBot.editMessageCallback(callbackQueryExt.getOriginal(), buildMessage(MESSAGE__SHOW_PLEASURE.replace(MESSAGE_UTILITY__RATE_TEMPLATE,rate)));
                    move();
                    reflection.setPleasure(rate);
                    // Wrap up callback handler
                    selfReflectionBot.answerCallback(callbackQueryExt.getOriginal());

                    // Next menu
                    selfReflectionBot.sendMenu(MESSAGE__RATE_VALUE, scaledRateKeyboard);
                }
            }

            /*
             * Stage #4 [Final] - Record value, save record to Google Sheets and give confirmation message
             */
            case STAGE.CONCLUSION -> {
                if (update.hasCallbackQuery()) {
                    CallbackQueryExt callbackQueryExt = CallbackQueryExt.extend(update.getCallbackQuery());

                    if (!callbackQueryExt.hasPurpose(ScaledRateMenu.getPurpose())) {
                        break;
                    }

                    // Handle menu callback - edit message and set rate
                    String rate = ScaledRateMenu.getRate(callbackQueryExt);
                    selfReflectionBot.editMessageCallback(callbackQueryExt.getOriginal(), buildMessage(MESSAGE__SHOW_VALUE.replace(MESSAGE_UTILITY__RATE_TEMPLATE,rate)));
                    move();
                    reflection.setValue(rate);
                    // Wrap up callback handler
                    selfReflectionBot.answerCallback(callbackQueryExt.getOriginal());

                    // Next actions
                    reflection.save();
                    selfReflectionBot.sendText(buildMessage(String.format(MESSAGE__SAVE_CONFIRMATION_FORMAT,
                                        reflection.getActivity(),
                                        reflection.getPleasure(),
                                        reflection.getValue())));
                    end();
                }
            }
        }
    }
}

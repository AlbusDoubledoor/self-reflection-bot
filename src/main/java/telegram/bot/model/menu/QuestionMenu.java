package telegram.bot.model.menu;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import telegram.bot.model.ext.CallbackQueryExt;

import java.util.List;

public class QuestionMenu {
    private final static String MENU_PURPOSE = "qm";
    private final static String MENU_BUTTON_ANSWER_POSITIVE_TEXT = "Да \u2705";
    private final static String MENU_BUTTON_ANSWER_POSITIVE_CALLBACK = "yes";
    private final static String MENU_BUTTON_ANSWER_NEGATIVE_TEXT = "Позже... \u274C";
    private final static String MENU_BUTTON_ANSWER_NEGATIVE_CALLBACK = "no";
    private final static String MENU_RESPONSE_NEGATIVE = "Оценка будет произведена позже... \uD83D\uDE1E";

    public static String getPurpose() {
        return MENU_PURPOSE;
    }

    public static InlineKeyboardMarkup getKeyboard(String menuId) {
        var answerPositive = InlineKeyboardButton.builder()
                .text(MENU_BUTTON_ANSWER_POSITIVE_TEXT)
                .callbackData(CallbackQueryExt.buildCallbackData(MENU_PURPOSE, menuId, MENU_BUTTON_ANSWER_POSITIVE_CALLBACK))
                .build();

        var answerNegative = InlineKeyboardButton.builder()
                .text(MENU_BUTTON_ANSWER_NEGATIVE_TEXT)
                .callbackData(CallbackQueryExt.buildCallbackData(MENU_PURPOSE, menuId, MENU_BUTTON_ANSWER_NEGATIVE_CALLBACK))
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(answerPositive))
                .keyboardRow(List.of(answerNegative))
                .build();
    }

    public static String getAnswerPositive() {
        return MENU_BUTTON_ANSWER_POSITIVE_CALLBACK;
    }

}

package com.kanashi.srb.telegram.bot.model.menu;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import com.kanashi.srb.telegram.bot.model.ext.CallbackQueryExt;

import java.util.List;

public class QuestionMenu extends TelegramMenu {
    private String menuId = "0";
    private final static String menuPurpose = "qm";
    private final static String MENU_BUTTON_ANSWER_POSITIVE_TEXT = "Да \u2705";
    private final static String MENU_BUTTON_ANSWER_POSITIVE_CALLBACK = "yes";
    private final static String MENU_BUTTON_ANSWER_NEGATIVE_TEXT = "Позже... \u274C";
    private final static String MENU_BUTTON_ANSWER_NEGATIVE_CALLBACK = "no";

    public static String getPurpose() {
        return menuPurpose;
    }

    @Override
    public InlineKeyboardMarkup getKeyboard() {
        var answerPositive = InlineKeyboardButton.builder()
                .text(MENU_BUTTON_ANSWER_POSITIVE_TEXT)
                .callbackData(CallbackQueryExt.buildCallbackData(menuPurpose, menuId, MENU_BUTTON_ANSWER_POSITIVE_CALLBACK))
                .build();

        var answerNegative = InlineKeyboardButton.builder()
                .text(MENU_BUTTON_ANSWER_NEGATIVE_TEXT)
                .callbackData(CallbackQueryExt.buildCallbackData(menuPurpose, menuId, MENU_BUTTON_ANSWER_NEGATIVE_CALLBACK))
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(answerPositive))
                .keyboardRow(List.of(answerNegative))
                .build();
    }

    public QuestionMenu(String menuId) {
        this.menuId = menuId;
    }

    public static String getAnswerPositive() {
        return MENU_BUTTON_ANSWER_POSITIVE_CALLBACK;
    }

}

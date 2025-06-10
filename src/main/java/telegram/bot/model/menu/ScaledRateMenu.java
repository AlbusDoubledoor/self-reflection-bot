package telegram.bot.model.menu;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import telegram.bot.model.ext.CallbackQueryExt;

import java.util.ArrayList;
import java.util.List;

public class ScaledRateMenu {
    private final static String MENU_PURPOSE = "scm";

    private static InlineKeyboardMarkup scaledRateKeyboard;

    public static String getPurpose() {
        return MENU_PURPOSE;
    }

    public static InlineKeyboardMarkup getKeyboard() {
        if (scaledRateKeyboard == null) {
            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();
            InlineKeyboardButton.InlineKeyboardButtonBuilder buttonBuilder = InlineKeyboardButton.builder();
            int counter = 1;
            for (int j = 1; j <= 2; ++j) {
                List<InlineKeyboardButton> keyboardButtonRow = new ArrayList<>();
                for (int i = 1; i <= 5; ++i) {
                    String rateValue = String.valueOf(counter++);
                    var nextButton = buttonBuilder
                            .text(rateValue).callbackData(CallbackQueryExt.buildCallbackData(MENU_PURPOSE, rateValue, rateValue));

                    keyboardButtonRow.add(nextButton.build());
                }
                keyboardBuilder.keyboardRow(keyboardButtonRow);
            }

            scaledRateKeyboard = keyboardBuilder.build();
        }

        return scaledRateKeyboard;
    }

    public static String getRate(CallbackQueryExt callbackQueryExt) {
        return callbackQueryExt.getCallbackPayload();
    }
}

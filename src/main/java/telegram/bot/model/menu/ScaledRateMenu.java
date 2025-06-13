package telegram.bot.model.menu;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import telegram.bot.model.ext.CallbackQueryExt;

import java.util.ArrayList;
import java.util.List;

public class ScaledRateMenu extends TelegramMenu {
    static {
        menuPurpose = "scm";
    }

    private static InlineKeyboardMarkup scaledRateKeyboard;

    @Override
    public InlineKeyboardMarkup getKeyboard() {
        return getSingleKeyboard();
    }

    private static InlineKeyboardMarkup getSingleKeyboard() {
        if (scaledRateKeyboard == null) {
            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();
            InlineKeyboardButton.InlineKeyboardButtonBuilder buttonBuilder = InlineKeyboardButton.builder();
            int counter = 1;
            for (int j = 1; j <= 2; ++j) {
                List<InlineKeyboardButton> keyboardButtonRow = new ArrayList<>();
                for (int i = 1; i <= 5; ++i) {
                    String rateValue = String.valueOf(counter++);
                    var nextButton = buttonBuilder
                            .text(rateValue).callbackData(CallbackQueryExt.buildCallbackData(menuPurpose, rateValue, rateValue));

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

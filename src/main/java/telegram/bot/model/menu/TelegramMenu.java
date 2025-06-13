package telegram.bot.model.menu;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public abstract class TelegramMenu  {
    protected static String menuPurpose = "";

    public static String getPurpose() {
        return menuPurpose;
    }

    public abstract InlineKeyboardMarkup getKeyboard();
}

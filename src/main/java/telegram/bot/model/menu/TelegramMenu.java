package telegram.bot.model.menu;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public abstract class TelegramMenu  {
    public abstract InlineKeyboardMarkup getKeyboard();
}

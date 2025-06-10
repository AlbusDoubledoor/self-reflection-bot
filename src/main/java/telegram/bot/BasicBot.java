package telegram.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public abstract class BasicBot extends TelegramLongPollingBot {
    private final String botUserName;

    public BasicBot(String botToken, String botUserName) {
        super(botToken);
        this.botUserName = botUserName;
    }

    @Override
    public String getBotUsername() {
        return this.botUserName;
    }

    public void sendText(Long who, String what){
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .parseMode("HTML")
                .text(what).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyMessage(Long who, Integer msgId){
        CopyMessage cm = CopyMessage.builder()
                .fromChatId(who.toString())
                .chatId(who.toString())
                .messageId(msgId)
                .build();
        try {
            execute(cm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMenu(Long who, String txt, InlineKeyboardMarkup kb){
        SendMessage sm = SendMessage.builder().chatId(who.toString())
                .text(txt)
                .parseMode("HTML")
                .replyMarkup(kb)
                .build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void editMessageCallback(CallbackQuery callbackQuery, String newText) {
        Long id = callbackQuery.getFrom().getId();
        Integer msgId = callbackQuery.getMessage().getMessageId();

        editMessage(id, msgId, newText);
    }

    public void editMessage(Long fromWhoId, Integer messageId, String newText) {
        EditMessageText newTxt = EditMessageText.builder()
                .chatId(fromWhoId.toString())
                .messageId(messageId)
                .text("")
                .build();

        newTxt.setText(newText);

        try {
            execute(newTxt);
        } catch (TelegramApiException e) {
            System.out.printf("Edit message exception [id=%s]. %s\n", messageId, e.getMessage());
        }
    }


    public void answerCallback(CallbackQuery callbackQuery) {
        String queryId = callbackQuery.getId();

        AnswerCallbackQuery close = AnswerCallbackQuery.builder()
                .callbackQueryId(queryId).build();

        try {
            execute(close);
        } catch (TelegramApiException e) {
            System.out.printf("Answer callback query [id=%s] exception. %s\n", queryId, e.getMessage());
        }
    }
}

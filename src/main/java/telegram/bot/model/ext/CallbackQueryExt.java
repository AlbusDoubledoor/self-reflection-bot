package telegram.bot.model.ext;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public class CallbackQueryExt extends CallbackQuery {
    private final static String EXTENSION_TAG = "&ext";
    private static final String EXTENSION_FORMAT = "[%s]p=%s/id=%s/msg=%s";
    private static final String EXTENSION_PATTERN = "\\[" + EXTENSION_TAG + "]p=(.+)/id=(.+)/msg=(.+$)";
    private final CallbackQuery callbackQuery;

    public CallbackQueryExt(CallbackQuery callbackQuery) {
        this.callbackQuery = callbackQuery;
    }

    public CallbackQuery getOriginal() {
        return callbackQuery;
    }

    public static CallbackQueryExt extend(CallbackQuery callbackQuery) {
        return new CallbackQueryExt(callbackQuery);
    }

    public boolean hasPurpose(String expectedCallbackPurpose) {
        String callbackData = callbackQuery.getData();

        String actualCallbackPurpose;
        if (callbackData.contains(EXTENSION_TAG)) {
            actualCallbackPurpose = callbackData.replaceAll(EXTENSION_PATTERN, "$1");
        } else {
            actualCallbackPurpose = callbackData;
        }

        return actualCallbackPurpose.equals(expectedCallbackPurpose);
    }

    public String getCallbackPayload() {
        String callbackData = callbackQuery.getData();

        if (callbackData.contains(EXTENSION_TAG)) {
            return callbackData.replaceAll(EXTENSION_PATTERN, "$3");
        }

        return callbackData;
    }

    public String getCallbackId() {
        String callbackData = callbackQuery.getData();

        if (callbackData.contains(EXTENSION_TAG)) {
            return callbackData.replaceAll(EXTENSION_PATTERN, "$2");
        }

        return callbackData;
    }

    public static String buildCallbackData(String purpose, String id, String data) {
        return String.format(EXTENSION_FORMAT, EXTENSION_TAG, purpose, id, data);
    }
}

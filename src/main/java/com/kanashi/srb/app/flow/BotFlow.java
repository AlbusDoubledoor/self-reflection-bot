package com.kanashi.srb.app.flow;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface BotFlow {
    // Flow control
    public void start();
    public void move();
    public void end();

    // Reactivity
    public void handleUpdate(Update update);
    public boolean isFinished();
}

package com.warks.tgbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final String TOKEN = "5248589134:AAG7EeLuXhxA_oRU3uEP-3A3HZTwNI-aAU4";

    private static final ConcurrentHashMap<PomodoroBot.Timer, Long> userTimers = new ConcurrentHashMap();

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        PomodoroBot bot = new PomodoroBot();
        telegramBotsApi.registerBot(bot);
        new Thread(() -> {
            try {
                bot.checkTimer();

            } catch (InterruptedException e) {
                System.out.println("Уппс");
            }
        }).run();
    }

    static class PomodoroBot extends TelegramLongPollingBot {


        @Override
        public String getBotUsername() {
            return "Pomodoro bot";
        }

        @Override
        public String getBotToken() {
            return TOKEN;
        }

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                if (update.getMessage().getText().equals("/start")) {
                    sendMsg("Pomodoro - сделай свое время более эффективным.\n Задай мне время работы, отдыха и количество итераций через пробел. Например, '5 3 3'. \n PS Я работаю пока в минутах", chatId.toString());
                } else {
                    var args = update.getMessage().getText().split(" ");
                    var isLegalAgs = true;

                    if (args.length != 3) isLegalAgs = false;
                    long workingTime = 0;
                    long breakingTime = 0;
                    int circles = 0;
                    try {
                        workingTime = Long.parseLong(args[0]);
                        breakingTime = Long.parseLong(args[1]);
                        circles = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sendMsg("Некорректный ввод", chatId.toString());
                        isLegalAgs = false;
                    }
                    if (isLegalAgs) {
                        sendMsg("Начинаю " + circles + " циклов по " + workingTime + " минут работы и " + breakingTime + " минут отдыха", chatId.toString());
                        sendMsg("Начинаем работу ", chatId.toString());
                        for (int i = 1; i <= circles; i++) {
                            var workTime = Instant.now().plus(workingTime + (workingTime + breakingTime) * (i - 1), ChronoUnit.MINUTES);
                            userTimers.put(new Timer(workTime, TimerType.WORK, false), chatId);
                            var breakTime = workTime.plus(breakingTime, ChronoUnit.MINUTES);
                            userTimers.put(new Timer(breakTime, TimerType.BREAK, i == circles ? true : false), chatId);
                            System.out.printf("add work timer  %s\n", workTime);
                            System.out.printf("add break timer %s\n", breakTime);
                        }
                    }
                }
            }
        }

        public void checkTimer() throws InterruptedException {
            while (true) {
                System.out.println("Active timers: " + userTimers.size());
                userTimers.forEach((timer, userId) -> {
                    System.out.printf("Checking userId = %d, server_time = %s, user_timer = %s\n", userId, Instant.now().toString(), timer.time.toString());
                    if (Instant.now().isAfter(timer.time)) {
                        userTimers.remove(timer);
                        switch (timer.timerType) {
                            case WORK -> sendMsg("Пора отдыхать", userId.toString());
                            case BREAK ->
                                    sendMsg((timer.isLast) ? "Таймер завершил свою работу" : "Пора работать", userId.toString());
                        }
                    }
                });
                Thread.sleep(1000);
            }
        }

        private void sendMsg(String text, String chatId) {
            SendMessage msg = new SendMessage();
            // пользователь чата
            msg.setChatId(chatId);
            msg.setText(text);

            try {
                execute(msg);
            } catch (TelegramApiException e) {
                System.out.println("Уппс");
            }
        }

        enum TimerType {
            WORK,
            BREAK
        }

        static record Timer(Instant time, TimerType timerType, boolean isLast) {
        }
    }

}

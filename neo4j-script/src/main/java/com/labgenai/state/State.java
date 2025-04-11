package com.labgenai.state;

import org.bsc.langgraph4j.prebuilt.MessagesState;
import dev.langchain4j.data.message.ChatMessage;

import java.util.Map;
import java.util.Optional;

public class State extends MessagesState<ChatMessage> {

    public Optional<String> next() {
        return this.value("next");
    }


    public State(Map<String, Object> initData) {
        super(initData);
    }
}
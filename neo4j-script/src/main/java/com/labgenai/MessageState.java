package com.labgenai;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import dev.langchain4j.data.message.ChatMessage;
import java.util.Map;

public class MessageState extends MessagesState<ChatMessage> {

    public MessageState(Map<String, Object> initData) {
        super( initData  );
    }


}
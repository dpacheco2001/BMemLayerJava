package com.labgenai;

import dev.langchain4j.data.message.ChatMessage;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.langchain4j.serializer.std.ChatMesssageSerializer;
import org.bsc.langgraph4j.langchain4j.serializer.std.ToolExecutionRequestSerializer;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import com.labgenai.state.State;
import com.labgenai.state.StateSerializer;
import java.util.Map;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import org.bsc.langgraph4j.checkpoint.MemorySaver; 
import org.bsc.langgraph4j.CompileConfig; 
import org.bsc.langgraph4j.RunnableConfig;
import java.util.Scanner;


import com.labgenai.node.RobertAssistant;

public class App {
    public static void main(String[] args) {
        ObjectStreamStateSerializer<MessageState> stateSerializer = new ObjectStreamStateSerializer<MessageState>( MessageState::new );
        stateSerializer.mapper()
            .register(ToolExecutionRequest.class, new ToolExecutionRequestSerializer() )
            .register(ChatMessage.class, new ChatMesssageSerializer());

        String apikeyGoogle= System.getenv("GOOGLE_API_KEY");




    
        OpenAiChatModel model45= OpenAiChatModel.builder()
        .apiKey( System.getenv("OPENAI_API_KEY") )
        .modelName( "gpt-4o" )
        .logResponses(true)
        .maxRetries(2)
        .temperature(0.0)
        .maxTokens(2000)
        .build();

        // Deepseek Sambanova
        OpenAiChatModel model = OpenAiChatModel.builder()
        .apiKey( "e96602c8-142c-4493-b2ea-64de90d4fbb4" )
        .baseUrl("https://api.sambanova.ai/v1")
        .modelName( "DeepSeek-V3-0324" )
        .logResponses(true)
        .maxRetries(2)
        .temperature(0.0)
        .maxTokens(2000)
        .build();

        OpenAiChatModel model3 = OpenAiChatModel.builder()
        .apiKey( "sk-cdfa6fd5e1e14cfd9b420082fc96ee50" )
        .baseUrl("https://api.deepseek.com/v1")
        .modelName( "deepseek-chat" )
        .logResponses(true)
        .maxRetries(2)
        .temperature(0.0)
        .maxTokens(2000)
        .build();

        GoogleAiGeminiChatModel model5 = GoogleAiGeminiChatModel.builder()
        .apiKey( apikeyGoogle)
        .modelName( "gemini-2.0-flash" )
        .maxRetries(2)
        .temperature(0.0)
        .maxOutputTokens(30)
        .logRequestsAndResponses(true)
        .build();


          
        try {

            var robert = new RobertAssistant(model);
            var workflow = new StateGraph<>( State.SCHEMA, new StateSerializer()  )
            .addNode( "robert", node_async(robert))
            .addEdge(START, "robert")
            .addEdge( "robert", END);
    
            MemorySaver memory = new MemorySaver();
            CompileConfig compileConfig = CompileConfig.builder()
                                .checkpointSaver(memory)
                                .build();
            
            var graph = workflow.compile(compileConfig);
            RunnableConfig runnableConfig =  RunnableConfig.builder()
            .threadId("conversation-num-1" )
            .build();
          
            //var response = graph.invoke( Map.of( "messages", UserMessage.from("Hola")),runnableConfig );
            //System.out.println( "Nodo: " + response);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Tú: ");
                String userInput = scanner.nextLine().trim();
                if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("salir")) {
                    System.out.println("Saliendo de la conversación.");
                    break;
                }

                System.out.println("Consultando al asistente...");
                for (var event : graph.stream(Map.of("messages", UserMessage.from(userInput)), runnableConfig)) {
                    System.out.println("Nodo: " + event);
                }
            }
            scanner.close();

            // for( var event : graph.stream( Map.of( "messages", UserMessage.from("Hola")),runnableConfig) ) {
            //     System.out.println( "Nodo: " + event);
            // }

        } catch (GraphStateException e) {
            e.printStackTrace(); 
        }


    }
}

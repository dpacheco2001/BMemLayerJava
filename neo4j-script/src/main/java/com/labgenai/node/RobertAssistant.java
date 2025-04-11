package com.labgenai.node;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;


import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.embedding.Embedding;

import org.bsc.langgraph4j.action.NodeAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.labgenai.state.*;

public class RobertAssistant implements NodeAction<State> {

    public static String searchMemory(String texto) {
        String pattern = "(?:```|´´´)\\s*(Episodica|Semantica)\\s*(.*?)\\s*(?:```|´´´)";
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(texto);
        final String uri = "bolt://localhost:7687";
        final String user = "neo4j";
        final String password = "mundial2022";

        if (matcher.find()) {
            String tipo = matcher.group(1).trim().toLowerCase();
            String contenido = matcher.group(2).trim();
            String modo = tipo.equals("episodica") ? "episodic" : "semantic";
            String indexName = "semantic_index";
            String ResultNodes =  "";
            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("text-embedding-3-small")
            .build();
            Response<Embedding> response = embeddingModel.embed(contenido);
            List<Float> query_embedded = response.content().vectorAsList();

            int k = 1;
            if (modo == "episodic"){
                indexName = "episodic_index";
            }
            else{
                indexName = "semantic_index";
            }
            
            String query = String.format(
                "CALL db.index.vector.queryNodes('%s', %d, %s) " +
                "YIELD node, score " +
                "RETURN node, score",
                indexName,
                k,
                query_embedded.toString()
            );
            

            try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))) {
                try (Session session = driver.session()) {
                    Result result = session.run(query);
                    for (Record record : result.list()) {
                        String nodeInformation = "";   
                        Node node = record.get("node").asNode();
                        String nodeId = node.id() + "";
                        Map<String,Object> propiedades = new HashMap<>(node.asMap());
                        propiedades.remove("embedding"); 
                        nodeInformation += "Node ID: " + nodeId + "\n" + 
                        "Node properties: " + propiedades + "\n";
                        ResultNodes += nodeInformation + "\n";
                    }
                }
    
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            System.out.println("ResultNodes: " + ResultNodes);
            return ResultNodes;
        }
        return null;
    }


    interface Service {
        @SystemMessage("""
    Eres el lobulo prefrontal de un humano, generas un plan de acción a partir del proceso asociativo
    de bloques de memoria (nodos en un knowledge graph en neo4j). Tu memoria consta de 3 módulos:
    - Semántica: En este módulo cada nodo representa los fundamentos y los conceptos, además de existir relaciones entre ellos.
    - Episódica: En este módulo cada nodo representa un evento o una experiencia,cada experiencia esta relacionada con nodos dentro de la memoria semántica.
    - De trabajo: En sí, es tu context window, lo que tienes en mente en este momento, es lo que te sirve para
    responder la pregunta del usuario.

    Tu memoria de trabajo esta estructurada de la siguiente forma:
    -Primer parrafo: ¿Quien soy? ¿Que tengo que hacer? ¿Como lo hago?
    -Segundo parrafo: Los dos últimos turnos de conversación entre el usuario y el asistente + resumen de la conversación fuera de los dos últimos turnos (esto para no perder el contexto de la conversación). Cuentese como turnos
    el input del usuario y todo hasta la respuesta final del asistente. Aca se encontra el ultimo input del usuario.
    -Tercer parrafo: Información de trabajo, lo que tienes en mente en este momento, es lo que te sirve para
    responder la pregunta del usuario. Si viene de la memoria episódica, te preguntarás, ¿Me sirve esto realmente para responder la pregunta?, si la respuesta es no, debes excarvar
    pero ahora en la memoria semántica, si la respuesta es si, entonces puedes responder la pregunta del usuario con los pasos que te dice la memoria episódica.

    ¿Como conseguiremos la información de trabajo?
    -Primero, tienes que excarvar en la memoria episódica. En la memoria episódica, los nodos representan experiencias, por ejemplo, si un usuario dice hola
     podriamos buscar en la memoria episódica un nodo que represente como hemos respondido que el usuario haya saludado. Los nodos en la memoria episodica estan estructurados de la siguiente manera:
     **Nombre del nodo**: Generalmente atribuido a una experiencia o evento.
     **Descripcion**: Un resumen de la experiencia o evento.
     **Pasos**: Un resumen de los pasos que se han seguido en la experiencia o evento.
     **Resultado**: Un valor entre 0 y 1, donde 0 significa que no ha tenido un resultado positivo los pasos que has relizado y 1 significa que los pasos que has realizado han desencadenado un resultado positivo.
    -Si lo que esta en la memoria episodica no srive, o ha tenido un resultado negativo. Entonces, DEBES excavar en la memoria semántica. En la memoria semántica, los nodos representan conceptos, por ejemplo, si un usuario dice ¿Qué ensayos hay en el laboratorio?
    podriamos buscar en la memoria semántica un nodo que diga ensayos disponibles, este nodo en sus propiedades dira que ensayos hay en el laboratorio, esto se almacenara en la memoria de trabajo y podremos usarlo para responder la pregunta del usuario.
    -Si lo que esta en la memoria semántica no nos sirve, como estas en un estado desarrolador, le dirás al usuario que no tienes la información para responder y de donde deberías sacar la información,
    de acuerdo a lo que te responda el desarrollador, escribiras en la memoria episodica o semántica.

    Para poder excarvar en los diferentes módulos de memorias, harás lo siguiente:

    -Si quieres excarvar en la memoria episódica, responderás:
     ´´´Episodica
        *Acciones que has hecho que se quieren encontrar*
     ´´´

     Ejemplo:
        ´´´Episodica
            Ya he respondido un saludo?
        ´´´
    
    -Si quieres excarvar en la memoria semántica, responderás:
    ´´´Semantica
        *Información que se quiere encontrar.*
    ´´´
    
    Ejemplo:
        ´´´Semantica
            *¿Que ensayos hay en el laboratorio?*
        ´´´

    Tendrás la capacidad de realizar pasos intermedios, es decir, puedes hacer un razonamiento y cuando consigas toda la información que necesitas, realizar tu respuesta final.
    Con razonamiento me refiero a lo siguiente:
    Razonamiento: El usuario me ha saludado, por lo que tengo que buscar en la memoria episodica si he respondido un saludo antes.
    ´´´Episodica
        Ya he respondido un saludo?
    ´´´´

    Después, en el siguiente input en la información de trabajo, tendrás el resultado de la memoria episodica a partir del query que mandaste y podrás utilizarlo para decidir
    si realizar otro paso intermedio o tu respuesta final.

    Ejemplo:
    --------------
    -Input: 
        >Quien eres? Eres robert, un asistente que ayudará al estudiante a resolver sus dudas.Estan en el laboratorio de materiales de la PUCP
        >Historial de conversación: 
                * User: hola
        >Información de trabajo: Todavía no hay información de trabajo, se ha realizado la primera interacción.Esperando razonamiento del asistente.

    -Razonamiento: El usuario me ha saludado,no veo que yo haya respondido en el historial, por lo que puedo deducir que nunca hemos hablado antes, buscaré en la memoria episodica para saber
    como abordar esta situación.
    ´´´Episodica
        Ya he respondido un saludo?
    ´´´

    -Input:
     >Quien eres? Eres robert, un asistente que ayudará al estudiante a resolver sus dudas.Estan en el laboratorio de materiales de la PUCP
     >Historial de conversación: 
            * User: hola
            * Razonamiento: El usuario me ha saludado,no veo que yo haya respondido en el historial, por lo que puedo deducir que nunca hemos hablado antes, buscaré en la memoria episodica para saber
            como abordar esta situación.
            ´´´Episodica
                Ya he respondido un saludo?
            ´´´
     >Información de trabajo: Información encontrada en la memoria episodica->
      Nodo: El usuario me saludo
      Descripcion: El usuario saludo y yo le dije que soy Robert, un asistente virtual que lo guiará a través de diferentes ensayos en el laboratorio de materiales de la pontificia universidad católica del perú
      Pasos: 1. El usuario me saludo 2. Respondí el saludo diciendole al usuario que soy Robert y diciendo que lo ayudaré a buscar al causa de la falla de la biela de la comañia SafeTravel, y le pregunté con que ensayo quisiera empezar.
      Resultado: 1

    -Respuesta final: Hola, soy Robert! Soy un asistente virtual diseñado para ayudarte en tus tareas. Veo que estamos aquí para indagar el caso de safetravel... la biela fracturada! ¿En qué puedo ayudarte hoy?

            
    *Nota: Maximo de pasos intermedios: 2, si no encuentras nada en la memoria episodica, entonces tienes que buscar en la memoria semántica, y si no encuentras nada en la memoria semántica, entonces le dices al usuario que no tienes la información para responder y de donde deberías sacar la información.
    *Nota: No puedes inventar información, si no encuentras nada en la memoria episodica o semántica, entonces le dices al usuario que no tienes la información para responder y de donde deberías sacar la información.
    *Siempre el inicio de tu respuesta final debe estar con 'Respuesta final:' y el inicio de tu razonamiento con 'Razonamiento:'. 
    *Nota: Nunca puedes hacer dos razonamientos en un mismo input, siempre uno por input, y nunca puedes excarvar en paralelo, siempre uno por uno.
    *Nota: Nunca debes poner nada despés de poner el query, cuando terminen los tres backticks, no puedes poner nada más, ni comillas, ni nada adicional.
""")
        String evaluate(@dev.langchain4j.service.UserMessage String humanmessage);
    }

    final Service service;

    public RobertAssistant( ChatLanguageModel model ){
        service = AiServices.builder( Service.class )
                .chatLanguageModel(model)
                .build();
    }
    @Override
    public Map<String, Object> apply(State state) {
        var messages = state.messages();
        String final_answer = "";
        String conversation_historial = "";
        for ( var m : messages ){
            var text = switch( m.type() ) {
                case USER -> ((UserMessage)m).singleText();
                case AI -> ((AiMessage)m).text();
                default -> throw new IllegalStateException("unexpected message type: " + m.type() );
                };
            conversation_historial += text + "\n";
        }
        while(true){
            String ResultNodes = "";
            var result = service.evaluate(conversation_historial);
            ResultNodes = searchMemory(result);
            if(ResultNodes != null){
                conversation_historial += result + "\n" + "Resultados del query solicitado:"+ ResultNodes + "\n";
            }
            else{
                final_answer = result;
                break;
            }
        }

        return Map.of( "messages", AiMessage.from(final_answer));

    }
}

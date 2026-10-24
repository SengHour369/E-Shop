package com.example.eshop.ai;
import com.example.eshop.ai.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AiConversationSeedContextTest {
 @Test void approvedExamplesArePassedToInferenceAlongsideCurrentMessageAndMode()throws Exception{
  var mapper=new ObjectMapper();var captured=new AtomicReference<String>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/api/v1/ai/conversation",exchange->{captured.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));byte[] response="{\"message\":\"Reviewed guidance\"}".getBytes();exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();});server.start();
  try{var examples=mock(AiTrainingExampleService.class);when(examples.retrieve("stock","en")).thenReturn(List.of(Map.of("prompt","stock example","response","check live inventory")));
   var service=new AiConversationService(mapper,"http://127.0.0.1:"+server.getAddress().getPort(),5,examples);
   assertThat(service.reply("stock","en",List.of(),true)).isEqualTo("Reviewed guidance");var body=mapper.readTree(captured.get());assertThat(body.path("message").asText()).isEqualTo("stock");assertThat(body.path("mode").asText()).isEqualTo("ADMIN");assertThat(body.path("examples").get(0).path("response").asText()).isEqualTo("check live inventory");
  }finally{server.stop(0);}
 }
}

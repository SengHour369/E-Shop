package com.example.eshop.ai.controller;

import com.example.eshop.ai.service.AiChatStateService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Media conversion only. Transcripts must be reviewed and submitted via the existing chat endpoint. */
@RestController
@RequestMapping("/api/ai/voice")
public class AiVoiceController {
    private final com.example.eshop.ai.voice.GeminiAudioTranscriptionService gemini;
    private final AiChatStateService state;
    private final com.example.eshop.ai.service.AiVoiceAudit audit;
    private final RestClient client;
    private final String url, key, probe;
    @Value("${ai.voice.model:}") private String model = "";
    public AiVoiceController(AiChatStateService state, com.example.eshop.ai.service.AiVoiceAudit audit,
            @Value("${ai.voice.transcription-url:}") String url,
            @Value("${ai.voice.api-key:}") String key,
            @Value("${ai.voice.ffprobe:ffprobe}") String probe,
            com.example.eshop.ai.voice.GeminiAudioTranscriptionService gemini) {
        this.gemini=gemini; this.audit=audit; this.state=state; this.url=url; this.key=key; this.probe=probe;
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(30000);
        client=RestClient.builder().requestFactory(factory).build();
    }
    public record Transcript(String text) {}
    @PostMapping(value="/transcriptions", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Transcript> transcribe(@RequestPart("audio") MultipartFile audio,
            @RequestParam(defaultValue="en") String language, HttpServletRequest request) {
        state.rateLimit(request.getRemoteAddr());
        if (!Set.of("en","km").contains(language)) throw failure(400,"Choose English or Khmer.");
        if (url.isBlank()) {
            if(!gemini.available())throw failure(503,"Voice transcription is not configured.");
            // The legacy custom provider may support guests; Google access requires an authenticated account.
            com.example.eshop.common.security.CurrentActor.userId();
        }
        if (audio.isEmpty() || audio.getSize()>5*1024*1024) throw failure(413,"Audio must be between 1 byte and 5 MB.");
        String type=Optional.ofNullable(audio.getContentType()).orElse("").split(";")[0];
        String extension=Map.of("audio/webm","webm","audio/mp4","mp4","audio/ogg","ogg","audio/wav","wav").get(type);
        if(extension==null) throw failure(415,"Unsupported recording format.");
        Path file=null, metadataFile=null; Process process=null;
        try {
            file=Files.createTempFile("eshop-voice-","."+extension);
            audio.transferTo(file);
            metadataFile=Files.createTempFile("eshop-voice-probe-",".json");
            // Decode metadata rather than trusting the browser's claimed recording duration.
            process=new ProcessBuilder(probe,"-v","error","-format_whitelist","matroska,webm,mov,mp4,m4a,3gp,3g2,mj2,ogg,wav","-protocol_whitelist","file,pipe","-select_streams","a:0",
                    "-show_entries","stream=codec_type,duration:format=duration:packet=pts_time,duration_time","-of","json",file.toString())
                    .redirectOutput(metadataFile.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(!process.waitFor(5,TimeUnit.SECONDS)) throw failure(400,"Unable to validate recording.");
            if(Files.size(metadataFile)>524288) throw failure(400,"Recording metadata exceeds limits.");
            byte[] metadata=Files.readAllBytes(metadataFile);
            var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(metadata);
            double duration=json.path("format").path("duration").asDouble(-1);
            if(duration<=0) duration=json.path("streams").path(0).path("duration").asDouble(-1);
            {
                for(var packet:json.path("packets")) duration=Math.max(duration,packet.path("pts_time").asDouble(0)+packet.path("duration_time").asDouble(0));
            }
            if(process.exitValue()!=0 || !"audio".equals(json.path("streams").path(0).path("codec_type").asText())
                    || !Double.isFinite(duration) || duration<=0 || duration>61) throw failure(400,"Recording must contain audio and be at most 60 seconds.");
            audit.accepted(language,audio.getSize());
            byte[] bytes=Files.readAllBytes(file);
            if(url.isBlank()) {
                // MP4 recordings use the M4A audio container MIME understood by Gemini.
                String mime=type.equals("audio/mp4")?"audio/m4a":type;
                return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Transcript(gemini.transcribe(bytes,mime,language)));
            }
            var body=new LinkedMultiValueMap<String,Object>();
            var headers=new HttpHeaders(); headers.setContentType(MediaType.parseMediaType(type));
            body.add("file",new HttpEntity<>(new ByteArrayResource(bytes){@Override public String getFilename(){return "recording."+extension;}},headers));
            body.add("language",language);
            if(!model.isBlank()) body.add("model",model);
            var call=client.post().uri(url).contentType(MediaType.MULTIPART_FORM_DATA);
            if(!key.isBlank()) call.header(HttpHeaders.AUTHORIZATION,"Bearer "+key);
            JsonNode result=call.body(body).retrieve().body(JsonNode.class);
            String text=result==null?"":result.path("text").asText("").trim();
            if(text.isBlank()) throw failure(422,"No speech detected. Please try again.");
            if(text.length()>4000) throw failure(422,"Transcript is too long. Please record a shorter question.");
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Transcript(text));
        } catch(ResponseStatusException e) { throw e;
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw failure(503,"Voice transcription interrupted.");
        } catch(Exception e) { throw failure(502,"Voice transcription unavailable. Please type your message or try again.");
        } finally {
            if(process!=null && process.isAlive()) process.destroyForcibly();
            if(file!=null) try { Files.deleteIfExists(file); } catch(Exception ignored) { }
            if(metadataFile!=null) try { Files.deleteIfExists(metadataFile); } catch(Exception ignored) { }
        }
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> transcriptionFailure(ResponseStatusException error){
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("status",error.getStatusCode().value(),"message",Optional.ofNullable(error.getReason()).orElse("Voice transcription unavailable.")));
    }
    private static ResponseStatusException failure(int code,String message){return new ResponseStatusException(HttpStatusCode.valueOf(code),message);}
}

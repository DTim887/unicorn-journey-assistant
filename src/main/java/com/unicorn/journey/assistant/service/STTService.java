package com.unicorn.journey.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.journey.assistant.client.AudioRequest;
import com.unicorn.journey.assistant.client.ByteDanceRecognizeClient;
import com.unicorn.journey.assistant.client.SSTResponse;
import com.unicorn.journey.assistant.protocol.EventType;
import com.unicorn.journey.assistant.protocol.Message;
import com.unicorn.journey.assistant.protocol.MsgType;
import com.unicorn.journey.assistant.protocol.SpeechWebSocketClient;
import com.unicorn.journey.assistant.utils.Base64Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class STTService {

    private final ByteDanceRecognizeClient byteDanceRecognizeClient;

    @Value("${doubao.app.id}")
    private String appId;

    @Value("${doubao.app.accesstoken}")
    private String accessToken;

    @Value("${doubao.sst.resource.id}")
    private String resourceId;

    @Value("${doubao.voice.endpoint}")
    private String voiceEndpoint;

    private static final String VOICE = "zh_male_zhubajie_mars_bigtts";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int SEQUENCE = -1;
    private static final String MODEL_NAME = "bigmodel";

    public STTService(ByteDanceRecognizeClient byteDanceRecognizeClient) {
        this.byteDanceRecognizeClient = byteDanceRecognizeClient;
    }

    public String speechToText(MultipartFile file) throws IOException {
        //组装豆包大模型语音转换请求
        AudioRequest audioRequest = AudioRequest.builder()
                .audio(new AudioRequest.Audio(Base64Utils.convertToBase64(file)))
                .user(new AudioRequest.User(appId))
                .request(new AudioRequest.Request(MODEL_NAME)).
                build();
        //组装返回
        SSTResponse sstResponse = byteDanceRecognizeClient.recognizeFlash(
                appId,
                accessToken,
                resourceId,
                SEQUENCE,
                audioRequest);
        return sstResponse.getResult().getText();
    }

    public void textToSpeech(String text) throws Exception {

        // Set request headers
        Map<String, String> headers = Map.of(
                "X-Api-App-Key", appId,
                "X-Api-Access-Key", accessToken,
                "X-Api-Resource-Id", voiceToResourceId(VOICE),
                "X-Api-Connect-Id", UUID.randomUUID().toString());

        // Create WebSocket client
        SpeechWebSocketClient client = new SpeechWebSocketClient(new URI(voiceEndpoint), headers);
        try {
            client.connectBlocking();
            // Prepare request parameters
            Map<String, Object> request = Map.of(
                    "user", Map.of("uid", UUID.randomUUID().toString()),
                    "req_params", Map.of(
                            "speaker", VOICE,
                            "audio_params", Map.of(
                                    "format", "wav",
                                    "sample_rate", 24000,
                                    "enable_timestamp", true),
                            // additions requires a JSON string
                            "additions", objectMapper.writeValueAsString(Map.of(
                                    "disable_markdown_filter", false)),
                            "text", text));

            // Send request
            client.sendFullClientMessage(objectMapper.writeValueAsBytes(request));

            // Receive response
            ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
            while (true) {
                Message msg = client.receiveMessage();
                log.info("Received message: {}", msg);

                if (msg.getType() == MsgType.AUDIO_ONLY_SERVER) {
                    if (msg.getPayload() != null) {
                        audioStream.write(msg.getPayload());
                    }
                } else if (msg.getType() == MsgType.ERROR) {
                    throw new RuntimeException("Server returned error: " + new String(msg.getPayload()));
                }

                if (msg.getType() == MsgType.FULL_SERVER_RESPONSE &&
                        msg.getEvent() == EventType.TTS_SENTENCE_END) {
                    String jsonString = new String(msg.getPayload(), StandardCharsets.UTF_8);
                    log.info("Received TTS response sentence end: {}", jsonString);
                    continue;
                }

                if (msg.getType() == MsgType.FULL_SERVER_RESPONSE &&
                        msg.getEvent() == EventType.SESSION_FINISHED) {
                    break;
                }
            }

            if (audioStream.size() == 0) {
                throw new RuntimeException("No audio data received");
            }

            // Save audio file
            String fileName = String.format("%s.%s", VOICE, "wav");
            Files.write(new File(fileName).toPath(), audioStream.toByteArray());
            log.info("Audio saved to file: {}", fileName);
        } finally {
            client.closeBlocking();
        }
    }


    /**
     * mvn compile exec:java -Dexec.mainClass=com.speech.volcengine.Bidirection -DappId=<appid> -DaccessToken=<access_token> -Dvoice=<voice_type> -Dtext="**你好**，我是豆包语音助手，很高兴认识你。这是一个愉快的旅程。"
     * @param text
     * @throws Exception
     */
//    public void textToSpeechStream(String text) throws Exception {
//        // Set request headers
//        Map<String, String> headers = Map.of(
//                "X-Api-App-Key", appId,
//                "X-Api-Access-Key", accessToken,
//                "X-Api-Resource-Id", resourceId.isEmpty() ? voiceToResourceId(VOICE) : resourceId,
//                "X-Api-Connect-Id", UUID.randomUUID().toString());
//
//        // Create WebSocket client
//        SpeechWebSocketClient client = new SpeechWebSocketClient(new URI("wss://openspeech.bytedance.com/api/v3/tts/bidirection"), headers);
//        try {
//            client.connectBlocking();
//            Map<String, Object> request = Map.of(
//                    "user", Map.of("uid", UUID.randomUUID().toString()),
//                    "namespace", "BidirectionalTTS",
//                    "req_params", Map.of(
//                            "speaker", VOICE,
//                            "audio_params", Map.of(
//                                    "format", "mp3",
//                                    "sample_rate", 24000,
//                                    "enable_timestamp", true),
//                            // additions requires a JSON string
//                            "additions", objectMapper.writeValueAsString(Map.of(
//                                    "disable_markdown_filter", false))));
//
//            // Start connection
//            client.sendStartConnection();
//            // Wait for connection started
//            client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.CONNECTION_STARTED);
//
//            // Process each sentence
//            String[] sentences = text.split("。");
//            boolean audioReceived = false;
//            for (int i = 0; i < sentences.length; i++) {
//                if (sentences[i].trim().isEmpty()) {
//                    continue;
//                }
//
//                String sessionId = UUID.randomUUID().toString();
//                ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
//
//                // Start session
//                Map<String, Object> startReq = Map.of(
//                        "user", request.get("user"),
//                        "namespace", request.get("namespace"),
//                        "req_params", request.get("req_params"),
//                        "event", EventType.START_SESSION.getValue());
//                client.sendStartSession(objectMapper.writeValueAsBytes(startReq), sessionId);
//                // Wait for session started
//                client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.SESSION_STARTED);
//
//                // Send text
//                for (char c : sentences[i].toCharArray()) {
//                    // Create new req_params with text
//                    @SuppressWarnings("unchecked")
//                    Map<String, Object> currentReqParams = new HashMap<>(
//                            (Map<String, Object>) request.get("req_params"));
//                    currentReqParams.put("text", String.valueOf(c));
//
//                    // Create current request
//                    Map<String, Object> currentRequest = Map.of(
//                            "user", request.get("user"),
//                            "namespace", request.get("namespace"),
//                            "req_params", currentReqParams,
//                            "event", EventType.TASK_REQUEST.getValue());
//
//                    client.sendTaskRequest(objectMapper.writeValueAsBytes(currentRequest), sessionId);
//                }
//
//                // End session
//                client.sendFinishSession(sessionId);
//
//                // Receive response
//                while (true) {
//                    Message msg = client.receiveMessage();
//                    switch (msg.getType()) {
//                        case FULL_SERVER_RESPONSE:
//                            break;
//                        case AUDIO_ONLY_SERVER:
//                            if (!audioReceived && audioStream.size() > 0) {
//                                audioReceived = true;
//                            }
//                            if (msg.getPayload() != null) {
//                                audioStream.write(msg.getPayload());
//                            }
//                            break;
//                        default:
//                            throw new RuntimeException("Unexpected message: " + msg);
//                    }
//                    if (msg.getEvent() == EventType.SESSION_FINISHED) {
//                        break;
//                    }
//                }
//
//                if (audioStream.size() > 0) {
//                    String fileName = String.format("%s_session_%d.%s", VOICE, i, "mp3");
//                    Files.write(new File(fileName).toPath(), audioStream.toByteArray());
//                    log.info("Audio saved to file: {}", fileName);
//                }
//            }
//
//            if (!audioReceived) {
//                throw new RuntimeException("No audio data received");
//            }
//
//            // End connection
//            client.sendFinishConnection();
//        } finally {
//            client.closeBlocking();
//        }
//    }


/**
 * Get resource ID based on voice type
 *
 * @param voice Voice type string
 * @return Corresponding resource ID
 */
public static String voiceToResourceId(String voice) {
    // Map different voice types to resource IDs based on actual needs
    if (voice.startsWith("S_")) {
        return "volc.megatts.default";
    }
    return "volc.service_type.10029";
}
}

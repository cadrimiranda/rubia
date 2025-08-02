package com.ruby.rubia_server.core.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.adapter.impl.ZApiAdapter;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.service.PhoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AudioMessageParsingTest extends AbstractIntegrationTest {
    
    private ZApiAdapter adapter;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        PhoneService phoneService = new PhoneService();
        adapter = new ZApiAdapter(phoneService);
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testAudioMessageParsing() throws Exception {
        String audioWebhookJson = """
            {
                "isStatusReply": false,
                "chatLid": "269161355821173@lid",
                "connectedPhone": "554891208536",
                "waitingMessage": false,
                "isEdit": false,
                "isGroup": false,
                "isNewsletter": false,
                "instanceId": "3E48B40A3ACEC048BB65C69C9520E8DB",
                "messageId": "3A8BB653A45850550DE4",
                "phone": "554891095462",
                "fromMe": false,
                "momment": 1753915341000,
                "status": "RECEIVED",
                "chatName": "Débora ",
                "senderPhoto": null,
                "senderName": "Débora Reitembach",
                "photo": "https://pps.whatsapp.net/v/t61.24694-24/example.jpg",
                "broadcast": false,
                "participantLid": null,
                "forwarded": false,
                "type": "ReceivedCallback",
                "fromApi": false,
                "audio": {
                    "ptt": true,
                    "seconds": 10,
                    "audioUrl": "https://f004.backblazeb2.com/file/temp-file-download/instances/3E48B40A3ACEC048BB65C69C9520E8DB/3A8BB653A45850550DE4/rOIOYdXI-JuxPnWTPsb8rQ==.ogg",
                    "mimeType": "audio/ogg; codecs=opus",
                    "viewOnce": false
                }
            }
            """;
            
        Map<String, Object> payload = objectMapper.readValue(audioWebhookJson, Map.class);
        
        IncomingMessage message = adapter.parseIncomingMessage(payload);
        
        assertNotNull(message);
        assertEquals("3A8BB653A45850550DE4", message.getMessageId());
        assertEquals("554891095462", message.getFrom());
        assertEquals("554891208536", message.getTo());
        assertEquals("269161355821173@lid", message.getChatLid());
        assertEquals("audio", message.getMediaType());
        assertEquals("audio/ogg; codecs=opus", message.getMimeType());
        assertEquals("https://f004.backblazeb2.com/file/temp-file-download/instances/3E48B40A3ACEC048BB65C69C9520E8DB/3A8BB653A45850550DE4/rOIOYdXI-JuxPnWTPsb8rQ==.ogg", 
                     message.getMediaUrl());
        assertEquals("z-api", message.getProvider());
        assertEquals("Débora Reitembach", message.getSenderName());
        assertFalse(message.isFromMe());
        assertNull(message.getBody()); // Audio messages don't have text content
        
        System.out.println("✅ Audio message parsed successfully!");
        System.out.println("Media Type: " + message.getMediaType());
        System.out.println("Media URL: " + message.getMediaUrl());
        System.out.println("MIME Type: " + message.getMimeType());
    }
}
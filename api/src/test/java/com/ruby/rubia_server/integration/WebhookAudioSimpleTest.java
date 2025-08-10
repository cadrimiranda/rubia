package com.ruby.rubia_server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.adapter.impl.ZApiAdapter;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.service.PhoneService;
import com.ruby.rubia_server.core.service.WhatsAppInstanceService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import com.ruby.rubia_server.core.validation.WhatsAppInstanceValidator;
import com.ruby.rubia_server.core.factory.ZApiUrlFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebhookAudioSimpleTest {

    private ZApiAdapter zApiAdapter;
    private ObjectMapper objectMapper;
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private WhatsAppInstanceService whatsAppInstanceService;
    
    @Mock
    private CompanyContextUtil companyContextUtil;
    
    @Mock
    private WhatsAppInstanceValidator instanceValidator;
    
    @Mock
    private ZApiUrlFactory urlFactory;

    @BeforeEach
    void setUp() {
        PhoneService phoneService = new PhoneService();
        zApiAdapter = new ZApiAdapter(restTemplate, phoneService, whatsAppInstanceService, companyContextUtil, instanceValidator, urlFactory);
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldParseRealZApiAudioWebhook() throws Exception {
        // Real Z-API audio payload that you provided
        String realAudioPayload = """
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
                "photo": "https://pps.whatsapp.net/v/t61.24694-24/395439499_371535991883790_5349058135386052333_n.jpg?ccb=11-4&oh=01_Q5Aa2AEcWe7mCVVm6jva1Phs60cSGxLuvKJASQAXb-tCk93amA&oe=6897AD2F&_nc_sid=5e03e0&_nc_cat=100",
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

        // Parse the JSON payload
        Map<String, Object> webhookPayload = objectMapper.readValue(realAudioPayload, Map.class);
        
        // Test that ZApiAdapter can parse the real audio payload
        IncomingMessage result = zApiAdapter.parseIncomingMessage(webhookPayload);
        
        // Assert that the audio message was parsed correctly
        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo("3A8BB653A45850550DE4");
        assertThat(result.getFrom()).isEqualTo("554891095462");
        assertThat(result.getTo()).isEqualTo("554891208536");
        assertThat(result.isFromMe()).isFalse();
        assertThat(result.getSenderName()).isEqualTo("Débora Reitembach");
        assertThat(result.getProvider()).isEqualTo("z-api");
        assertThat(result.getMediaType()).isEqualTo("audio");
        
        System.out.println("✅ Real Z-API audio webhook parsed successfully!");
    }
}
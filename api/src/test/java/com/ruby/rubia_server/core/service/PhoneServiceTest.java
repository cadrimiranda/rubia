package com.ruby.rubia_server.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.assertThat;

class PhoneServiceTest {

    private PhoneService phoneService;

    @BeforeEach
    void setUp() {
        phoneService = new PhoneService();
    }

    @Test
    void shouldNormalizePhoneWithoutCountryCode() {
        // 11 digits (with 9th digit)
        assertThat(phoneService.normalize("11999887766")).isEqualTo("5511999887766");
        
        // 10 digits (without 9th digit)
        assertThat(phoneService.normalize("1199887766")).isEqualTo("551199887766");
    }

    @Test
    void shouldNormalizePhoneWithCountryCodeNoPlus() {
        assertThat(phoneService.normalize("5511999887766")).isEqualTo("5511999887766");
    }

    @Test
    void shouldNormalizePhoneWithCountryCodeAndPlus() {
        assertThat(phoneService.normalize("+5511999887766")).isEqualTo("5511999887766");
    }

    @Test
    void shouldNormalizePhoneWithFormatting() {
        assertThat(phoneService.normalize("(11) 99988-7766")).isEqualTo("5511999887766");
        assertThat(phoneService.normalize("+55 (11) 9 9988-7766")).isEqualTo("5511999887766");
    }

    @Test
    void shouldReturnNullForNullOrEmpty() {
        assertThat(phoneService.normalize(null)).isNull();
        assertThat(phoneService.normalize("")).isNull();
        assertThat(phoneService.normalize("   ")).isNull();
    }

    @Test
    void shouldValidatePhoneNumbers() {
        // Valid formats
        assertThat(phoneService.isValid("+5511999887766")).isTrue();
        assertThat(phoneService.isValid("5511999887766")).isTrue();
        assertThat(phoneService.isValid("11999887766")).isTrue();
        
        // Invalid formats
        assertThat(phoneService.isValid(null)).isFalse();
        assertThat(phoneService.isValid("")).isFalse();
        assertThat(phoneService.isValid("123")).isFalse();
    }

    @Test
    void shouldExtractFromProvider() {
        assertThat(phoneService.extractFromProvider("whatsapp:+5511999887766"))
                .isEqualTo("5511999887766");
        assertThat(phoneService.extractFromProvider("+5511999887766"))
                .isEqualTo("5511999887766");
    }

    @Test
    void shouldFormatForZApi() {
        assertThat(phoneService.formatForZApi("+5511999887766")).isEqualTo("5511999887766");
        assertThat(phoneService.formatForZApi("11999887766")).isEqualTo("5511999887766");
    }

    @Test
    void shouldFormatForTwilio() {
        String phone = "+5511999887766";
        
        // With whatsapp: prefix in from number
        assertThat(phoneService.formatForTwilio(phone, "whatsapp:+5511888888888"))
                .isEqualTo("whatsapp:" + phone);
        
        // Without whatsapp: prefix in from number
        assertThat(phoneService.formatForTwilio(phone, "+5511888888888"))
                .isEqualTo(phone);
        
        // Phone already has whatsapp: prefix
        assertThat(phoneService.formatForTwilio("whatsapp:" + phone, "whatsapp:+5511888888888"))
                .isEqualTo("whatsapp:" + phone);
    }

    @Test
    void shouldGenerateDefaultName() {
        assertThat(phoneService.generateDefaultName("+5511999887766"))
                .isEqualTo("WhatsApp 7766");
        assertThat(phoneService.generateDefaultName("123"))
                .isEqualTo("WhatsApp 123");
        assertThat(phoneService.generateDefaultName("12"))
                .isEqualTo("WhatsApp 12");
        assertThat(phoneService.generateDefaultName(""))
                .isEqualTo("WhatsApp User");
    }

    @Test
    void shouldCheckPhoneEquivalence() {
        assertThat(phoneService.areEquivalent("11999887766", "+5511999887766")).isTrue();
        assertThat(phoneService.areEquivalent("(11) 99988-7766", "5511999887766")).isTrue();
        assertThat(phoneService.areEquivalent("11999887766", "11999887777")).isFalse();
    }
}
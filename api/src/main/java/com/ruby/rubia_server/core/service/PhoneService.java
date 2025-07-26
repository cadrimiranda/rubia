package com.ruby.rubia_server.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço centralizado para operações relacionadas a números de telefone.
 * 
 * Responsabilidades:
 * - Normalização de números de telefone para formato padrão (+55DDDnúmero)
 * - Validação de formato de números
 * - Formatação específica para diferentes providers (Z-API, Twilio, etc.)
 * - Extração de números de diferentes formatos de entrada
 */
@Service
@Slf4j
public class PhoneService {

    /**
     * Formato padrão brasileiro: +55DDDnúmero
     * Exemplo: +5511999887766
     */
    private static final String BRAZILIAN_COUNTRY_CODE = "55";
    private static final String NORMALIZED_PREFIX = "+55";

    /**
     * Normaliza um número de telefone para o formato padrão brasileiro: +55DDDnúmero
     * 
     * @param phone número de telefone em qualquer formato
     * @return número normalizado no formato +55DDDnúmero ou null se inválido
     */
    public String normalize(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        
        // Remove all non-digit characters
        String digitsOnly = phone.replaceAll("\\D", "");
        
        // Handle Brazilian phone numbers with standard format +55DDDnúmero
        if (digitsOnly.length() == 10 || digitsOnly.length() == 11) {
            // Brazilian phone without country code (DDDnúmero)
            return NORMALIZED_PREFIX + digitsOnly;
        } else if (digitsOnly.length() == 12 && digitsOnly.startsWith(BRAZILIAN_COUNTRY_CODE)) {
            // Brazilian phone with country code but without + (55DDDnúmero)
            return "+" + digitsOnly;
        } else if (digitsOnly.length() == 13 && digitsOnly.startsWith(BRAZILIAN_COUNTRY_CODE)) {
            // Phone with + removed during regex (was +55DDDnúmero)
            return "+" + digitsOnly;
        }
        
        // For unexpected formats, log warning and attempt normalization
        log.warn("Unexpected phone format: '{}' (digits: '{}'). Attempting normalization.", phone, digitsOnly);
        
        // Try to extract Brazilian number from international formats
        if (digitsOnly.startsWith(BRAZILIAN_COUNTRY_CODE) && digitsOnly.length() >= 12) {
            return "+" + digitsOnly.substring(0, 13); // Keep +55 + 11 digits max
        }
        
        return NORMALIZED_PREFIX + digitsOnly; // Fallback: assume it's Brazilian
    }

    /**
     * Valida se um número de telefone está em formato válido
     * 
     * @param phone número de telefone para validar
     * @return true se válido, false caso contrário
     */
    public boolean isValid(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        // Remove any spaces and formatting
        String cleanPhone = phone.replaceAll("\\s+", "");
        
        // Must match Brazilian format: +5511999999999 or 5511999999999 (11-15 digits total)
        return cleanPhone.matches("^\\+?[1-9]\\d{10,14}$");
    }

    /**
     * Extrai número de telefone removendo prefixos específicos de providers
     * 
     * @param providerNumber número com prefixo específico (ex: whatsapp:+5511999887766)
     * @return número limpo
     */
    public String extractFromProvider(String providerNumber) {
        if (providerNumber == null) {
            return null;
        }
        
        // Remove 'whatsapp:' prefix if present (Twilio)
        String cleanNumber = providerNumber.replace("whatsapp:", "");
        
        return normalize(cleanNumber);
    }

    /**
     * Formata número para Z-API (sem + e sem whatsapp:)
     * Formato Z-API: 5511999887766
     * 
     * @param phone número normalizado
     * @return número formatado para Z-API
     */
    public String formatForZApi(String phone) {
        if (phone == null) {
            return null;
        }

        String digitsOnly = phone.replaceAll("\\D", "");
        
        if (digitsOnly.startsWith(BRAZILIAN_COUNTRY_CODE)) {
            return digitsOnly;
        } else {
            return BRAZILIAN_COUNTRY_CODE + digitsOnly;
        }
    }

    /**
     * Formata número para Twilio (com whatsapp: prefix se necessário)
     * 
     * @param phone número normalizado
     * @param fromNumber número de origem configurado no Twilio
     * @return número formatado para Twilio
     */
    public String formatForTwilio(String phone, String fromNumber) {
        if (fromNumber != null && fromNumber.startsWith("whatsapp:")) {
            return phone.startsWith("whatsapp:") ? phone : "whatsapp:" + phone;
        }
        return phone;
    }

    /**
     * Gera nome padrão baseado no número de telefone
     * 
     * @param phone número de telefone normalizado
     * @return nome padrão (ex: "WhatsApp 7766")
     */
    public String generateDefaultName(String phone) {
        if (phone == null) {
            return "WhatsApp User";
        }
        
        // Extract only digits for getting last digits
        String digitsOnly = phone.replaceAll("\\D", "");
        
        if (digitsOnly.length() < 4) {
            return digitsOnly.isEmpty() ? "WhatsApp User" : "WhatsApp " + digitsOnly;
        }
        
        String lastDigits = digitsOnly.substring(digitsOnly.length() - 4);
        return "WhatsApp " + lastDigits;
    }

    /**
     * Verifica se dois números de telefone são equivalentes
     * 
     * @param phone1 primeiro número
     * @param phone2 segundo número
     * @return true se são equivalentes após normalização
     */
    public boolean areEquivalent(String phone1, String phone2) {
        String normalized1 = normalize(phone1);
        String normalized2 = normalize(phone2);
        
        return normalized1 != null && normalized1.equals(normalized2);
    }
}
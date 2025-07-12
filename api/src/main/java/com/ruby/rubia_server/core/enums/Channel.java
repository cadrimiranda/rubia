package com.ruby.rubia_server.core.enums;

/**
 * Canal de comunicação - mapeado como ordinal para PostgreSQL
 * WHATSAPP = 0, INSTAGRAM = 1, FACEBOOK = 2, WEB_CHAT = 3, EMAIL = 4
 */
public enum Channel {
    WHATSAPP,  // 0 - Canal de comunicação via WhatsApp
    INSTAGRAM, // 1 - Canal de comunicação via Instagram
    FACEBOOK,  // 2 - Canal de comunicação via Facebook
    WEB_CHAT,  // 3 - Canal de comunicação via chat no website
    EMAIL      // 4 - Canal de comunicação via e-mail
}
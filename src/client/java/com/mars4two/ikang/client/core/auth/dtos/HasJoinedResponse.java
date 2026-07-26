package com.mars4two.ikang.client.core.auth.dtos;

/**
 * Response DTO for the device authorization flow returned by /mod-auth/has-joined.
 * Mirrors the OAuth 2.0 Device Authorization Response (RFC 8628).
 */
public record HasJoinedResponse(String verificationUrl, String deviceCode, int interval, int expiresIn) {
}

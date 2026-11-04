package com.mars4two.ikang.core.auth.dtos;

public record GetModCredentialsRequest(String profileId, String serverId, String joinOtp, String serverName) {
}

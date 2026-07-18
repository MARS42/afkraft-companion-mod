package com.mars4two.ikang.client.command;

import java.util.UUID;

public record ConnectRequestContent(UUID profileId, UUID serverId) {
}

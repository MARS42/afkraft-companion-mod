package com.mars4two.ikang.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class AFKraftCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("afkraft")
                .then(Commands.literal("connect")
                    .executes(AFKraftCommand::executeConnect))
        );
    }
    
    private static int executeConnect(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Enviar mensaje al jugador
        source.sendSuccess(() -> Component.literal("Iniciando enlace..."), false);
        
        // TODO: Aquí irá la lógica de conexión cuando esté lista
        // Por ejemplo: iniciar conexión con el servidor, autenticación, etc.
        
        return 1; // Retornar 1 indica éxito
    }
}

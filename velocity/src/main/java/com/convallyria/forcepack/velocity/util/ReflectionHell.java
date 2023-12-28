package com.convallyria.forcepack.velocity.util;

import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @deprecated Remove when velocity finishes resource pack API
 */
@Deprecated(forRemoval = true)
public class ReflectionHell {

    private static final Method WRITE_METHOD;
    private static final Field PLAYER_CONNECTION;
    private static final Constructor<?> REMOVE_RESOURCE_PACK_CONSTRUCTOR;

    static {
        try {
            final Class<?> connectedPlayerClass = Class.forName("com.velocitypowered.proxy.connection.client.ConnectedPlayer");
            final Class<?> connectionClass = Class.forName("com.velocitypowered.proxy.connection.MinecraftConnection");
            WRITE_METHOD = connectionClass.getDeclaredMethod("write", Object.class);
            PLAYER_CONNECTION = connectedPlayerClass.getDeclaredField("connection");
            PLAYER_CONNECTION.setAccessible(true);
            Class<?> removeResourcePack = Class.forName("com.velocitypowered.proxy.protocol.packet.RemoveResourcePack");
            REMOVE_RESOURCE_PACK_CONSTRUCTOR = removeResourcePack.getConstructor(UUID.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeResourcePack(@Nullable UUID uuid, Player player) {
        try {
            final Object connectionObject = PLAYER_CONNECTION.get(player);
            WRITE_METHOD.invoke(connectionObject, REMOVE_RESOURCE_PACK_CONSTRUCTOR.newInstance(uuid));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

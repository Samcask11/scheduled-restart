package samcask.scheduledrestart.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import samcask.scheduledrestart.RestartNoPlayerScheduler;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
	@Final
	@Shadow
    private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
	public void playerConnected(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
		RestartNoPlayerScheduler.playerConnected();
	}

	@Inject(method = "remove", at = @At("TAIL"))
	private void playerDisconnected(ServerPlayerEntity player, CallbackInfo info) {
		RestartNoPlayerScheduler.playerDisconnected(server);
	}
}
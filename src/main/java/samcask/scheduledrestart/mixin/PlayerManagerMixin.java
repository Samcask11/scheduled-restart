package samcask.scheduledrestart.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import samcask.scheduledrestart.RestartNoPlayerScheduler;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
	@Shadow public abstract MinecraftServer getServer();

	@Inject(method = "onPlayerConnect", at = @At("HEAD"))
	private void playerConnected(CallbackInfo info) {
		RestartNoPlayerScheduler.playerConnected();
	}

	@Inject(method = "remove", at = @At("TAIL"))
	private void playerDisconnected(CallbackInfo info) {
		RestartNoPlayerScheduler.playerDisconnected(getServer());
	}
}
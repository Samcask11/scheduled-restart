package samcask.scheduledrestart;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledRestart implements ModInitializer {
	public static final String MOD_ID = "scheduled-restart";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static RestartConfig config;

	@Override
	public void onInitialize() {
		AutoConfig.register(RestartConfig.class, JanksonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(RestartConfig.class).getConfig();

		CommandRegistrationCallback.EVENT.register(RestartCommand::register);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (config.restartScheduleType.equals("daily") && config.restartInterval > 0) {
				RestartAutoScheduler.scheduleAutoRestartDaily(server, config.dailyRestartTimes);
			}
			else if (config.restartScheduleType.equals("interval") && config.restartInterval > 0) {
				RestartAutoScheduler.scheduleAutoRestartInterval(server, config.restartInterval);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(RestartNoPlayerScheduler::finalPlayerDisconnected);
	}

	public static void logInfo(String message) {
		ScheduledRestart.LOGGER.info("[ScheduledRestartMod] - " + message);
	}

	public static void logError(String message) {
		ScheduledRestart.LOGGER.error("[ScheduledRestartMod] - " + message);
	}

	public static void sendAnnouncement(MinecraftServer server, String message, boolean logMessage) {
		for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.of("[Server] " + message), false);
		}
		if (logMessage) logInfo(message);
	}
}
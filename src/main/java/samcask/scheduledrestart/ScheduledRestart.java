package samcask.scheduledrestart;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
			if (config.restartScheduleType.equals("daily") && config.restartInterval > 0) RestartHandler.scheduleAutoRestartDaily(server, config.dailyRestartTimes);
			else if (config.restartScheduleType.equals("interval") && config.restartInterval > 0) RestartHandler.scheduleAutoRestartInterval(server, config.restartInterval);
		});
	}

	int hi = 5;

	public static void logInfo(String message) {
		ScheduledRestart.LOGGER.info("[ScheduledRestartMod] - " + message);
	}

	public static void logError(String message) {
		ScheduledRestart.LOGGER.error("[ScheduledRestartMod] - " + message);
	}
}
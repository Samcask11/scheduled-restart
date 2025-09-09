package samcask.scheduledrestart;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = ScheduledRestart.MOD_ID)
public class RestartConfig implements ConfigData {
    @Comment("Name of script file to start the server.\nIf left blank, defaults to start.bat on windows, or start.sh otherwise.")
    String startScriptPath = "";
    @Comment("Message displayed to kicked players on server restart.")
    String kickMessage = "The server is restarting";

    @Comment("Which type of automatic restart scheduling to use. Leave blank to disable automatic scheduled restarts.")
    String restartScheduleType = "daily";
    @Comment("Restarts the server at these times each day.\nOnly works if restartScheduleType is set to \"daily\".")
    String[] dailyRestartTimes = {"00:00"};
    @Comment("Restarts the server after it as been online for this many seconds.\nOnly works if restartScheduleType is set to \"interval\".")
    int restartInterval = 86400;

    @Comment("How many seconds in advance all players should be warned before a scheduled server restart.")
    int[] restartWarningTimes = {300,30};

    @Comment("Automatically restarts the server after no players have been online for this many seconds.\nSet to 0 to disable restarting while no players are online.")
    int noPlayerRestartDelay = 3600;
}

package samcask.scheduledrestart.config;

import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;

public class RestartConfig extends ConfigData {
    public String startScriptPath;
    public String kickMessage;
    public String restartScheduleType;
    public ArrayList<LocalTime> dailyRestartTimes;
    public int restartInterval;
    public ArrayList<Integer> restartWarningTimes;
    public int noPlayerRestartDelay;

    private String startScriptPathDefault = "";
    private String kickMessageDefault = "The server is restarting";
    private String restartScheduleTypeDefault = "daily";
    private String dailyRestartTimesDefault = "00:00";
    private String restartIntervalDefault = "86400";
    private String restartWarningTimesDefault = "300,30";
    private String noPlayerRestartDelayDefault = "3600";

    public RestartConfig(File configFile) {
        loadProperties(configFile);
        setConfigDataValues();
        saveProperties(configFile);
    }

    @Deprecated
    public RestartConfig(File configFile, OldConfigData oldConfigData) {
        loadProperties(configFile);
        loadOldConfigData(oldConfigData);
        setConfigDataValues();
        saveProperties(configFile);
    }

    @Deprecated
    private void loadOldConfigData(OldConfigData oldConfigData) {
        startScriptPathDefault = oldConfigData.extractString("startScriptPath", startScriptPathDefault);
        kickMessageDefault = oldConfigData.extractString("kickMessage", kickMessageDefault);
        restartScheduleTypeDefault = oldConfigData.extractString("restartScheduleType", restartScheduleTypeDefault);
        dailyRestartTimesDefault = oldConfigData.extractTimeArray("dailyRestartTimes", dailyRestartTimesDefault);
        restartIntervalDefault = oldConfigData.extractInteger("restartInterval", restartIntervalDefault);
        restartWarningTimesDefault = oldConfigData.extractIntegerArray("restartWarningTimes", restartWarningTimesDefault);
        noPlayerRestartDelayDefault = oldConfigData.extractInteger("noPlayerRestartDelay", noPlayerRestartDelayDefault);
    }

    protected void setConfigDataValues() {
        startScriptPath = addStringProperty("start-script-path", startScriptPathDefault,
                "Name of script file to start the server.\nIf left blank, defaults to start.bat on windows, or start.sh otherwise.");
        kickMessage = addStringProperty("kick-message", kickMessageDefault,
                "Message displayed to kicked players on server restart.");
        restartScheduleType = addStringProperty("restart-schedule-type", restartScheduleTypeDefault,
                "Which type of automatic restart scheduling to use. Leave blank to disable automatic scheduled restarts.");
        dailyRestartTimes = addTimeArrayProperty("daily-restart-times", dailyRestartTimesDefault,
                "Restarts the server at these times each day.\nOnly works if restartScheduleType is set to \"daily\".");
        restartInterval = addIntegerProperty("restart-interval", restartIntervalDefault,
                "Restarts the server after it has been online for this many seconds.\nOnly works if restartScheduleType is set to \"interval\".");
        restartWarningTimes = addIntegerArrayProperty("restart-warning-times", restartWarningTimesDefault,
                "How many seconds in advance all players should be warned before a scheduled server restart.");
        noPlayerRestartDelay = addIntegerProperty("no-player-restart-delay", noPlayerRestartDelayDefault,
                "Automatically restarts the server after no players have been online for this many seconds.\nSet to 0 to disable.");
    }
}

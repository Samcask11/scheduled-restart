package samcask.scheduledrestart;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Properties;

public class RestartConfig {
    public StringProperty startScriptPath = new StringProperty("start-script-path", "",
            "Name of script file to start the server.\nIf left blank, defaults to start.bat on windows, or start.sh otherwise.");
    public StringProperty kickMessage = new StringProperty("kick-message", "The server is restarting",
            "Message displayed to kicked players on server restart.");
    public StringProperty restartScheduleType = new StringProperty("restart-schedule-type", "daily",
            "Which type of automatic restart scheduling to use. Leave blank to disable automatic scheduled restarts.");
    public TimeArrayProperty dailyRestartTimes = new TimeArrayProperty("daily-restart-times", "00:00",
            "Restarts the server at these times each day.\nOnly works if restartScheduleType is set to \"daily\".");
    public IntegerProperty restartInterval = new IntegerProperty("restart-interval", "86400",
            "Restarts the server after it has been online for this many seconds.\nOnly works if restartScheduleType is set to \"interval\".");
    public IntegerArrayProperty restartWarningTimes = new IntegerArrayProperty("restart-warning-times", "300,30",
            "How many seconds in advance all players should be warned before a scheduled server restart.");
    public IntegerProperty noPlayerRestartDelay = new IntegerProperty("no-player-restart-delay", "3600",
            "Automatically restarts the server after no players have been online for this many seconds.\nSet to 0 to disable restarting while no players are online.");

    private Properties properties;
    private ArrayList<ConfigProperty<?>> propertyList = new ArrayList<>();

    public RestartConfig(File configFile) {
        loadProperties(configFile);
        saveProperties(configFile);
    }

    public RestartConfig(File configFile, OldRestartConfig oldConfigData) {
        loadProperties(configFile);
        loadOldConfigData(oldConfigData);
        saveProperties(configFile);
    }

    private void loadProperties(File configFile) {
        properties = new Properties();
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) {
            ScheduledRestart.logError("Failed to read config file: " + e);
        }

        propertyList.add(startScriptPath);
        propertyList.add(kickMessage);
        propertyList.add(restartScheduleType);
        propertyList.add(dailyRestartTimes);
        propertyList.add(restartInterval);
        propertyList.add(restartWarningTimes);
        propertyList.add(noPlayerRestartDelay);

        for (ConfigProperty<?> property : propertyList) {
            property.loadValue();
        }
    }

    public void loadOldConfigData(OldRestartConfig oldConfigData) {
        startScriptPath.setString(oldConfigData.extractValue("startScriptPath", new char[] { '\"' }, startScriptPath.value));
        kickMessage.setString(oldConfigData.extractValue("kickMessage", new char[] { '\"' }, kickMessage.value));
        restartScheduleType.setString(oldConfigData.extractValue("restartScheduleType", new char[] { '\"' }, restartScheduleType.value));
        dailyRestartTimes.setString(oldConfigData.extractValue("dailyRestartTimes", new char[] { ']' }, dailyRestartTimes.value.toString()));
        restartInterval.setString(oldConfigData.extractValue("restartInterval", new char[] { ',', '\n' }, restartInterval.value.toString()));
        restartWarningTimes.setString(oldConfigData.extractValue("restartWarningTimes", new char[] { ']' }, restartWarningTimes.value.toString()));
        noPlayerRestartDelay.setString(oldConfigData.extractValue("noPlayerRestartDelay", new char[] { ',', '\n' }, noPlayerRestartDelay.value.toString()));
    }

    private void saveProperties(File configFile) {
        try {
            StringBuilder newConfig = new StringBuilder();

            for (ConfigProperty<?> property : propertyList) {
                newConfig.append(property);
            }

            configFile.delete();
            configFile.getParentFile().mkdirs();
            Files.createFile(configFile.toPath());
            PrintWriter writer = new PrintWriter(configFile, StandardCharsets.UTF_8);
            writer.write(newConfig.toString());
            writer.close();
        } catch (Exception e) {
            ScheduledRestart.logError("Failed to write to config file: " + e);
        }
    }

    public abstract class ConfigProperty<T> {
        public T value;

        private final String key;
        private final String defaultValue;
        private final String comment;

        public ConfigProperty(String key, String defaultValue, String comment) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.comment = comment;
        }

        public void setString(String value) {
            try {
                this.value = parse(value);
            } catch (Exception e) {
                this.value = parse(defaultValue);
            }
        }

        public void loadValue() {
            setString(properties.getProperty(key).replaceAll("^ +| +$", ""));
        }

        protected abstract T parse(String value);

        @Override
        public String toString() {
            return "# " + comment.replaceAll("\n", "\n# ") + "\n" + key + "=" + value + "\n\n";
        }
    }

    public class StringProperty extends ConfigProperty<String> {

        public StringProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        @Override
        public String parse(String value) {
            if (value == null) throw new NullPointerException();
            return value;
        }
    }

    public class IntegerProperty extends ConfigProperty<Integer> {

        public IntegerProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        @Override
        public Integer parse(String value) {
            return Integer.parseInt(value);
        }
    }

    public class TimeProperty extends ConfigProperty<LocalTime> {

        public TimeProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        @Override
        public LocalTime parse(String value) {
            return LocalTime.parse(value);
        }
    }

    public abstract class ArrayProperty<T> extends ConfigProperty<ArrayList<T>> {
        public ArrayProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        public ArrayList<T> parse(String value) {
            String[] items = value.replaceAll("[]\\[\n ]", "").split(",");
            ArrayList<T> values = new ArrayList<>();
            for (int i = 0; i < items.length; i++) {
                values.add(parseElement(items[i]));
            }
            return values;
        }

        public abstract T parseElement(String value);
    }

    public class IntegerArrayProperty extends ArrayProperty<Integer> {

        public IntegerArrayProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        @Override
        public Integer parseElement(String value) {
            return Integer.parseInt(value);
        }
    }

    public class TimeArrayProperty extends ArrayProperty<LocalTime> {

        public TimeArrayProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        @Override
        public LocalTime parseElement(String value) {
            return LocalTime.parse(value);
        }
    }
}

package samcask.scheduledrestart.config;

import samcask.scheduledrestart.ScheduledRestart;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Properties;
import java.util.function.Function;

public abstract class ConfigData {
    private Properties properties;
    private ArrayList<ConfigProperty<?>> propertyList = new ArrayList<>();

    protected void loadProperties(File configFile) {
        properties = new Properties();
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) {
            ScheduledRestart.logError("Failed to read config file: " + e);
        }
    }

    protected abstract void setConfigDataValues();

    protected void saveProperties(File configFile) {
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

    protected String addStringProperty(String key, String defaultValue, String comment) {
        StringProperty property = new StringProperty(key, defaultValue, comment);
        propertyList.add(property);
        property.load();
        return property.value;
    }

    protected Integer addIntegerProperty(String key, String defaultValue, String comment) {
        IntegerProperty property = new IntegerProperty(key, defaultValue, comment);
        propertyList.add(property);
        property.load();
        return property.value;
    }

    protected LocalTime addTimeProperty(String key, String defaultValue, String comment) {
        TimeProperty property = new TimeProperty(key, defaultValue, comment);
        propertyList.add(property);
        property.load();
        return property.value;
    }

    protected ArrayList<Integer> addIntegerArrayProperty(String key, String defaultValue, String comment) {
        IntegerArrayProperty property = new IntegerArrayProperty(key, defaultValue, comment);
        propertyList.add(property);
        property.load();
        return property.value;
    }

    protected ArrayList<LocalTime> addTimeArrayProperty(String key, String defaultValue, String comment) {
        TimeArrayProperty property = new TimeArrayProperty(key, defaultValue, comment);
        propertyList.add(property);
        property.load();
        return property.value;
    }

    protected abstract class ConfigProperty<T> {
        public T value;

        private final String key;
        private final String defaultValue;
        private final String comment;

        private ConfigProperty(String key, String defaultValue, String comment) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.comment = comment;
        }

        public void load() {
            try {
                this.value = parse(properties.getProperty(key).replaceAll("^ +| +$", ""));
            } catch (Exception e) {
                this.value = parse(defaultValue);
            }
        }

        protected abstract T parse(String value);

        @Override
        public String toString() {
            return "# " + comment.replaceAll("\n", "\n# ") + "\n" + key + "=" + value + "\n\n";
        }
    }

    protected class StringProperty extends ConfigProperty<String> {

        private StringProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        protected String parse(String value) {
            if (value == null) throw new NullPointerException();
            return value.replaceAll("\"", "");
        }
    }

    protected class IntegerProperty extends ConfigProperty<Integer> {

        private IntegerProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        protected Integer parse(String value) {
            return Integer.parseInt(value);
        }
    }

    protected class TimeProperty extends ConfigProperty<LocalTime> {

        private TimeProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        protected LocalTime parse(String value) {
            return LocalTime.parse(value);
        }
    }

    protected abstract class ArrayProperty<T> extends ConfigProperty<ArrayList<T>> {
        private ArrayProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        protected ArrayList<T> parse(String value) {
            return parseArray(value, this::parseElement);
        }

        protected abstract T parseElement(String value);
    }

    protected class IntegerArrayProperty extends ArrayProperty<Integer> {

        protected IntegerArrayProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        protected Integer parseElement(String value) {
            return Integer.parseInt(value);
        }
    }

    protected class TimeArrayProperty extends ArrayProperty<LocalTime> {

        protected TimeArrayProperty(String key, String defaultValue, String comment) {
            super(key, defaultValue, comment);
        }

        protected LocalTime parseElement(String value) {
            return LocalTime.parse(value);
        }
    }

    protected static <T> ArrayList<T> parseArray(String value, Function<String, T> parse) {
        String[] items = value.replaceAll("[\\[\\]\"\\s]", "").split(",");
        ArrayList<T> values = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            values.add(parse.apply(items[i]));
        }
        return values;
    }
}

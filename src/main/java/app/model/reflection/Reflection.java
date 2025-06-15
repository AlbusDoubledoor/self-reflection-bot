package app.model.reflection;

import app.utility.DateTextFormatter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class Reflection implements Serializable {
    private String pleasure = "";
    private String value = "";
    private String activity = "";
    private String targetTimePeriod;
    private final LocalDateTime dateTime;
    private final long timestamp;
    private final String id = UUID.randomUUID().toString().replace("-","");

    public Reflection(LocalDateTime dateTime) {
        timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();

        int hour = dateTime.getHour();

        if (hour == 0) {
            dateTime = dateTime.minusDays(1);
        }

        this.dateTime = dateTime;
        this.targetTimePeriod = DateTextFormatter.getTimePeriod(hour);
    }

    public Reflection(LocalDateTime dateTime, @Nonnull String customTimePeriod) {
        this(dateTime);
        this.targetTimePeriod = customTimePeriod;
    }

    public void save() {
        ReflectionWriter sheetsWriter =
                new ReflectionWriter.Builder(this)
                                        .sheets()
                                        .build();

        sheetsWriter.write();
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getId(){
        return id;
    }

    public String getActivity() {
        return activity;
    }

    public String getPleasure() {
        return pleasure;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTargetTimePeriod() {
        return targetTimePeriod;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setPleasure(String pleasure) {
        this.pleasure = pleasure;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

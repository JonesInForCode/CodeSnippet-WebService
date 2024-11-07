package platform.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "code_snippets")
public class CodeSnippet {
    @Id
    private UUID id;
    private String code;
    private LocalDateTime date;
    private long time;
    private int views;
    private boolean restricted;
    private boolean restrictedByTime;
    private boolean restrictedByViews;

    public CodeSnippet() {
        this.id = UUID.randomUUID();
        this.date = LocalDateTime.now();
    }

    public CodeSnippet(String code, Long time, Integer views) {
        this();
        this.code = code;
        setRestrictions(time, views);
        this.date = LocalDateTime.now(); // Ensure date is set when snippet is created
    }

    public void setRestrictions(Long time, Integer views) {
        this.time = (time != null && time > 0) ? time : 0;
        this.views = (views != null && views > 0) ? views : 0;
        this.restrictedByTime = (this.time > 0);
        this.restrictedByViews = (this.views > 0);
        this.restricted = (restrictedByTime || restrictedByViews);
    }

    public CodeSnippet updateAndReturn() {
        if (this.restricted) {
            if (this.restrictedByTime) {
                long secondsDiff = ChronoUnit.SECONDS.between(this.date, LocalDateTime.now());
                this.time = Math.max(0, this.time - secondsDiff);
                this.date = LocalDateTime.now(); // Update the date to the current time after calculation
            }
            if (this.restrictedByViews && this.views > 0) {
                this.views--;
            }
        }
        return this;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.date = LocalDateTime.now();
    }

    public boolean isExpired() {
        return (restrictedByTime && time <= 0) || (restrictedByViews && views <= 0);
    }

    public long getRemainingTime() {
        if (restrictedByTime) {
            long secondsDiff = ChronoUnit.SECONDS.between(this.date, LocalDateTime.now());
            return Math.max(0, this.time - secondsDiff);
        }
        return 0;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public boolean isRestrictedByTime() {
        return restrictedByTime;
    }

    public boolean isRestrictedByViews() {
        return restrictedByViews;
    }

}

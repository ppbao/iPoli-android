package io.ipoli.android.app.settings.events;

import org.threeten.bp.DayOfWeek;

import java.util.Set;

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 12/19/16.
 */
public class WorkDaysChangedEvent {
    public final Set<DayOfWeek> workDays;

    public WorkDaysChangedEvent(Set<DayOfWeek> workDays) {
        this.workDays = workDays;
    }
}

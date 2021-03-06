package io.ipoli.android.app.sync;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.squareup.otto.Bus;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.ipoli.android.app.events.AppErrorEvent;
import io.ipoli.android.app.utils.DateUtils;
import me.everything.providers.android.calendar.Calendar;
import me.everything.providers.android.calendar.CalendarProvider;
import me.everything.providers.android.calendar.Event;
import me.everything.providers.android.calendar.Reminder;
import me.everything.providers.core.Data;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 5/8/16.
 */
public class SyncAndroidCalendarProvider extends CalendarProvider {
    private static final int PROJECTION_EVENT_ID_INDEX = 0;
    private static final int PROJECTION_START_MINUTE_INDEX = 1;
    private static final int PROJECTION_BEGIN_INDEX = 2;
    private static final int PROJECTION_END_INDEX = 3;

    private final ContentResolver contentResolver;
    private final Bus eventBus;

    private static final String[] INSTANCE_PROJECTION = new String[]{
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.START_MINUTE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
    };

    public SyncAndroidCalendarProvider(Context context, Bus eventBus) {
        super(context);
        contentResolver = context.getContentResolver();
        this.eventBus = eventBus;
    }

    public List<Event> getDeletedEvents(long calendarId) {
        String selection = CalendarContract.Events.CALENDAR_ID + " = ? AND "
                + CalendarContract.Events.DELETED + "= ?";
        String[] selectionArgs = new String[]{String.valueOf(calendarId), String.valueOf(1)};
        Data<Event> data = getContentTableData(Event.uri, selection, selectionArgs, null, Event.class);
        return data == null ? new ArrayList<>() : data.getList();
    }


    public Map<Event, List<InstanceData>> getCalendarEvents(long calendarId, LocalDate startDate, LocalDate endDate) {

        String selection = CalendarContract.Instances.CALENDAR_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(calendarId)};
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, DateUtils.toMillis(startDate));
        ContentUris.appendId(builder, DateUtils.toMillis(endDate));

        List<InstanceData> instances = new ArrayList<>();

        Cursor cur = contentResolver.query(builder.build(), INSTANCE_PROJECTION, selection, selectionArgs, null);
        if (cur == null) {
            return new HashMap<>();
        }
        try {
            while (cur.moveToNext()) {
                long eventId = cur.getLong(PROJECTION_EVENT_ID_INDEX);
                int startMinute = cur.getInt(PROJECTION_START_MINUTE_INDEX);
                long begin = cur.getLong(PROJECTION_BEGIN_INDEX);
                long end = cur.getLong(PROJECTION_END_INDEX);
                instances.add(new InstanceData(eventId, startMinute, begin, end));
            }
        } catch (Exception e) {
            eventBus.post(new AppErrorEvent(e));
        } finally {
            cur.close();
        }

        if (instances.isEmpty()) {
            return new HashMap<>();
        }

        Map<Event, List<InstanceData>> result = new HashMap<>();
        Map<Long, Event> idToEvent = new HashMap<>();

        for (InstanceData i : instances) {
            if (idToEvent.containsKey(i.eventId)) {
                result.get(idToEvent.get(i.eventId)).add(i);
            } else {
                Event e = getEvent(i.eventId);
                if (e == null) {
                    eventBus.post(new AppErrorEvent(
                            new RuntimeException("Google calendar event with id: " + i.eventId + " not found")));
                    continue;
                }
                idToEvent.put(e.id, e);
                List<InstanceData> instanceList = new ArrayList<>();
                instanceList.add(i);
                result.put(e, instanceList);
            }
        }
        return result;
    }

    public List<Calendar> getAndroidCalendars() {
        Data<Calendar> data = getCalendars();
        return data == null ? new ArrayList<>() : data.getList();
    }

    public List<Reminder> getEventReminders(long eventId) {
        Data<Reminder> data = getReminders(eventId);
        if (data == null) {
            return new ArrayList<>();
        }
        List<Reminder> reminders = new ArrayList<>();
        try {
            reminders = data.getList();
        } catch (Exception e) {
            eventBus.post(new AppErrorEvent(e));
        }
        return reminders;
    }

}

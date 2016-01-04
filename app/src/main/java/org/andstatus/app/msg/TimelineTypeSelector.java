package org.andstatus.app.msg;

import android.content.Context;

import org.andstatus.app.data.TimelineType;

public class TimelineTypeSelector {
    private static TimelineType[] timelineTypes = {
            TimelineType.HOME,
            TimelineType.FAVORITES,
            TimelineType.MENTIONS,
            TimelineType.DIRECT,
            TimelineType.USER,
            TimelineType.FOLLOWING_USER,
            TimelineType.PUBLIC,
            TimelineType.EVERYTHING,
            TimelineType.DRAFTS,
            TimelineType.OUTBOX
    };
    
    private Context context;
    
    public TimelineTypeSelector(Context context) {
        super();
        this.context = context;
    }

    public CharSequence[] getTitles() {
        CharSequence[] titles = new String[timelineTypes.length];
        for (int ind=0; ind < timelineTypes.length; ind++) {
            titles[ind] = timelineTypes[ind].getTitle(context);
        }
        return titles;
    }

    public TimelineType positionToType(int position) {
        TimelineType type = TimelineType.UNKNOWN;
        if (position >= 0 && position < timelineTypes.length) {
            type = timelineTypes[position];
        }
        return type;
    }

    public static TimelineType selectableType(TimelineType typeSelected) {
        TimelineType typeSelectable = TimelineType.HOME;
        for (TimelineType type : timelineTypes) {
            if (type == typeSelected) {
                typeSelectable = typeSelected;
                break;
            }
        }
        return typeSelectable;
    }
}

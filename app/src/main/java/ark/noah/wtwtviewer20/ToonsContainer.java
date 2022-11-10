package ark.noah.wtwtviewer20;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class ToonsContainer implements Parcelable {
    boolean isDebug = true;

    public int dbID;
    public String toonName, toonType, thumbnailURL;
    public int toonID, episodeID, releaseWeekdays;
    public boolean hide, completed;

    public ToonsContainer(
            int dbID,
            String  toonName,
            String  toonType,
            int     toonID,
            int     episodeID,
            int     releaseWeekdays,
            boolean hide,
            boolean completed,
            String thumbnailURL
    ) {
        isDebug = MainActivity.Instance.isDebug;

        this.dbID = dbID;
        this.toonName = toonName;
        this.toonType = toonType;
        this.toonID = toonID;
        this.episodeID = episodeID;
        this.releaseWeekdays = releaseWeekdays;
        this.hide = hide;
        this.completed = completed;
        this.thumbnailURL = thumbnailURL;
    }

    protected ToonsContainer(Parcel in) {
        dbID = in.readInt();
        toonName = in.readString();
        toonType = in.readString();
        toonID = in.readInt();
        episodeID = in.readInt();
        releaseWeekdays = in.readInt();
        hide = in.readBoolean();
        completed = in.readBoolean();
        thumbnailURL = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dbID);
        dest.writeString(toonName);
        dest.writeString(toonType);
        dest.writeInt(toonID);
        dest.writeInt(episodeID);
        dest.writeInt(releaseWeekdays);
        dest.writeBoolean(hide);
        dest.writeBoolean(completed);
        dest.writeString(thumbnailURL);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @param flag an 1 that is bit-shifted leftward in an amount of ReleaseDay.getValue()
     * @return true if inserted, false if removed
     */
    public boolean tryChangeFlagWeekday(int flag) {
        if((releaseWeekdays & flag) == 0) { //toon was known to releasing not on the day flag indicates. enabling
            releaseWeekdays |= flag;
            return true;
        } else {                            //toon was known to releasing on the day flag indicates. disabling
            releaseWeekdays &= ~flag;
            return false;
        }
    }

    public Integer[] getAllReleaseDaysInArray() {
        ArrayList<Integer> releaseDays = new ArrayList<>();

        if((releaseWeekdays & (1 << ReleaseDay.SUN.getValue())) != 0) releaseDays.add(Calendar.SUNDAY);
        if((releaseWeekdays & (1 << ReleaseDay.MON.getValue())) != 0) releaseDays.add(Calendar.MONDAY);
        if((releaseWeekdays & (1 << ReleaseDay.TUE.getValue())) != 0) releaseDays.add(Calendar.TUESDAY);
        if((releaseWeekdays & (1 << ReleaseDay.WED.getValue())) != 0) releaseDays.add(Calendar.WEDNESDAY);
        if((releaseWeekdays & (1 << ReleaseDay.THU.getValue())) != 0) releaseDays.add(Calendar.THURSDAY);
        if((releaseWeekdays & (1 << ReleaseDay.FRI.getValue())) != 0) releaseDays.add(Calendar.FRIDAY);
        if((releaseWeekdays & (1 << ReleaseDay.SAT.getValue())) != 0) releaseDays.add(Calendar.SATURDAY);

        return releaseDays.toArray(new Integer[0]);
    }

    public String getAllReleaseDaysInString() {
        String releaseDays = "";

        if((releaseWeekdays & (1 << ReleaseDay.SUN.getValue())) != 0) releaseDays += "SUN";
        if((releaseWeekdays & (1 << ReleaseDay.MON.getValue())) != 0) releaseDays += releaseDays.length() > 0 ? ", MON" :  "MON";
        if((releaseWeekdays & (1 << ReleaseDay.TUE.getValue())) != 0) releaseDays += releaseDays.length() > 0 ? ", TUE" :  "TUE";
        if((releaseWeekdays & (1 << ReleaseDay.WED.getValue())) != 0) releaseDays += releaseDays.length() > 0 ? ", WED" :  "WED";
        if((releaseWeekdays & (1 << ReleaseDay.THU.getValue())) != 0) releaseDays += releaseDays.length() > 0 ? ", THU" :  "THU";
        if((releaseWeekdays & (1 << ReleaseDay.FRI.getValue())) != 0) releaseDays += releaseDays.length() > 0 ? ", FRI" :  "FRI";
        if((releaseWeekdays & (1 << ReleaseDay.SAT.getValue())) != 0) releaseDays += releaseDays.length() > 0 ? ", SAT" :  "SAT";

        return releaseDays;
    }

    public String asLink() {
        String link = LinkGetter.Instance.getEntryPoint() + toonType + 2 + "?toon=" + toonID + "&num=" + episodeID;
        if(isDebug) Log.i("DebugLog", link);
        return link;
    }

    public ToonsContainer getNext() {
        return new ToonsContainer(dbID, toonName, toonType, toonID, episodeID + 1, releaseWeekdays, hide, completed, thumbnailURL);
    }

    public ToonsContainer getPrev() {
        return new ToonsContainer(dbID, toonName, toonType, toonID, episodeID - 1, releaseWeekdays, hide, completed, thumbnailURL);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ToonsContainer) {
            ToonsContainer other = (ToonsContainer) o;
            if(!isSameToon(other)) return false;
            if(other.episodeID != episodeID) return false;
            if(!other.toonType.equals(toonType)) return false;
            if(other.releaseWeekdays!=releaseWeekdays) return false;
            if(other.hide != hide) return false;
            if(other.completed != completed) return false;
            if(!other.thumbnailURL.equals(thumbnailURL)) return false;
            if(!other.toonName.equals(toonName)) return false;
        }
        return super.equals(o);
    }

    public boolean isSameToon(ToonsContainer other) {
        return ((other.dbID == dbID) && (other.toonID == toonID));
    }

    public enum ReleaseDay {
        NON(0),
        SUN(1),
        MON(2),
        TUE(3),
        WED(4),
        THU(5),
        FRI(6),
        SAT(7),
        ALL(8);

        private final int val;

        ReleaseDay(int val) {
            this.val = val;
        }

        public int getValue() {
            return val;
        }

        public int asFlag() {
            return 1 << getValue();
        }

        public static int convertBitindexValueToNormal(int value) {
            Log.i("DebugLog", "value: " + value);
            if(value <= 0) return value;
            int count = 0;
            while(value != 1) {
                value = (value >> 1);
                ++count;
            }
            Log.i("DebugLog", "count: " + count);
            return count;
        }

        public static ReleaseDay getDayFromCalendarDayOfWeek(int dayOfWeek) {
            switch(dayOfWeek)
            {
                default:
                case Calendar.SUNDAY:
                    return SUN;
                case Calendar.MONDAY:
                    return MON;
                case Calendar.TUESDAY:
                    return TUE;
                case Calendar.WEDNESDAY:
                    return WED;
                case Calendar.THURSDAY:
                    return THU;
                case Calendar.FRIDAY:
                    return FRI;
                case Calendar.SATURDAY:
                    return SAT;
            }
        }
    }

    public static final Creator<ToonsContainer> CREATOR = new Creator<ToonsContainer>() {
        @Override
        public ToonsContainer createFromParcel(Parcel in) {
            return new ToonsContainer(in);
        }

        @Override
        public ToonsContainer[] newArray(int size) {
            return new ToonsContainer[size];
        }
    };
}

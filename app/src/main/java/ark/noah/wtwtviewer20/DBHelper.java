package ark.noah.wtwtviewer20;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Content.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME_TOONS = "Table_toons";
    public static final String ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_TYPE = "type";
    public static final String COL_TOONID = "toonid";
    public static final String COL_EPIID = "epiid";
    public static final String COL_RELEASEDAY = "releaseweekday";
    public static final String COL_HIDE = "hide";
    public static final String COL_COMPLETE = "complete";
    public static final String COL_THUMBNAILURL = "thumbnailurl";

    private final String createQueryToons
            = "CREATE TABLE IF NOT EXISTS "+ TABLE_NAME_TOONS +"("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT, "
                + COL_TYPE + " TEXT, "
                + COL_TOONID + " INTEGER, "
                + COL_EPIID + " INTEGER, "
                + COL_RELEASEDAY + " INTEGER, "
                + COL_HIDE + " INTEGER DEFAULT 0, "
                + COL_COMPLETE + " INTEGER DEFAULT 0, "
                + COL_THUMBNAILURL + " TEXT )";

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(createQueryToons);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        //do something
//        if (oldVersion < 2) {
//        }
    }

    @SuppressLint("Range")
    public ArrayList<ToonsContainer> getAllToonsFiltered(ToonsContainer.ReleaseDay releaseDay, boolean showHidden, boolean showCompleted) {
        ArrayList<ToonsContainer> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selectAllQueryFilterByDay
                = "SELECT ( SELECT COUNT(*) FROM " + TABLE_NAME_TOONS + " AS T2 WHERE T2." + ID + " <= T1." + ID + " ) AS row_Num, * " + " FROM " + TABLE_NAME_TOONS + " AS T1";
        boolean isAllWeekday = (releaseDay == ToonsContainer.ReleaseDay.ALL);
        if(!showHidden) {
            selectAllQueryFilterByDay += (" WHERE " + COL_HIDE + " IS " + 0);
            if(!showCompleted) {
                selectAllQueryFilterByDay += (" AND " + COL_COMPLETE + " IS " + 0);
                if(!isAllWeekday) {
                    selectAllQueryFilterByDay += (" AND ( " + COL_RELEASEDAY + " & " + (1 << releaseDay.getValue()) + " ) IS NOT " + 0);
                }
            }
        }
        else if(!showCompleted) {
            selectAllQueryFilterByDay += (" WHERE " + COL_COMPLETE + " IS " + 0);
            if(!isAllWeekday) {
                selectAllQueryFilterByDay += (" AND ( " + COL_RELEASEDAY + " & " + (1 << releaseDay.getValue()) + " ) IS NOT " + 0);
            }
        } else if(!isAllWeekday) {
            selectAllQueryFilterByDay += (" WHERE ( " + COL_RELEASEDAY + " & " + (1 << releaseDay.getValue()) + " ) IS NOT " + 0);
        }
        Cursor cursor = db.rawQuery(selectAllQueryFilterByDay, null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                do {
                    list.add(
                            new ToonsContainer(
                                    cursor.getInt   (cursor.getColumnIndex(ID)),
                                    cursor.getString(cursor.getColumnIndex(COL_TITLE)),
                                    cursor.getString(cursor.getColumnIndex(COL_TYPE)),
                                    cursor.getInt   (cursor.getColumnIndex(COL_TOONID)),
                                    cursor.getInt   (cursor.getColumnIndex(COL_EPIID)),
                                    cursor.getInt   (cursor.getColumnIndex(COL_RELEASEDAY)),
                                    cursor.getInt   (cursor.getColumnIndex(COL_HIDE)) != 0,
                                    cursor.getInt   (cursor.getColumnIndex(COL_COMPLETE)) != 0,
                                    cursor.getString(cursor.getColumnIndex(COL_THUMBNAILURL))
                            )
                    );
                } while(cursor.moveToNext());
            }
            cursor.close();
        }
        db.close();
        return list;
    }

    public void insertToonContent(ToonsContainer container) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        ContentValues contentValues = new ContentValues();
//        contentValues.put(ID, container.dbID);
        contentValues.put(COL_TITLE, container.toonName);
        contentValues.put(COL_TYPE, container.toonType);
        contentValues.put(COL_TOONID, container.toonID);
        contentValues.put(COL_EPIID, container.episodeID);
        contentValues.put(COL_RELEASEDAY, container.releaseWeekdays);
        contentValues.put(COL_HIDE, container.hide);
        contentValues.put(COL_COMPLETE, container.completed);
        contentValues.put(COL_THUMBNAILURL, container.thumbnailURL);

        db.insert(TABLE_NAME_TOONS, null, contentValues);

        db.setTransactionSuccessful();
        db.endTransaction();

        db.close();
    }

    public int editToonContent(ToonsContainer container) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_TITLE, container.toonName);
        contentValues.put(COL_TYPE, container.toonType);
        contentValues.put(COL_TOONID, container.toonID);
        contentValues.put(COL_EPIID, container.episodeID);
        contentValues.put(COL_RELEASEDAY, container.releaseWeekdays);
        contentValues.put(COL_HIDE, container.hide);
        contentValues.put(COL_COMPLETE, container.completed);
        contentValues.put(COL_THUMBNAILURL, container.thumbnailURL);

        int toReturn = db.update(TABLE_NAME_TOONS, contentValues, ID + "='" + container.dbID + "'", null);

        db.setTransactionSuccessful();
        db.endTransaction();

        db.close();

        return toReturn;
    }

    public int deleteToonContent(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int toReturn = db.delete(TABLE_NAME_TOONS, ID + "='" + id + "'", null);
        db.close();
        return toReturn;
    }

//    @SuppressLint("Range")
//    public int getToonIDAtLastPosition() {
//        SQLiteDatabase db = getReadableDatabase();
//        int id = -1;
//        Cursor cursor = db.rawQuery(selectAllQuery, null);
//        if(cursor != null) {
//            if(cursor.moveToFirst()) {
//                do {
//                    id = cursor.getInt(cursor.getColumnIndex(ID));
//                } while(cursor.moveToNext());
//            }
//            cursor.close();
//        }
//        db.close();
//        return id;
//    }
}

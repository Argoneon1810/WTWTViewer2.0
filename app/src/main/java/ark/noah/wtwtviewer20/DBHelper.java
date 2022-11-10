package ark.noah.wtwtviewer20;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

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

    public static final String TABLE_NAME_EPISODES = "Table_episodes";
//    public static final String COL_TOONID = "toonid";
    public static final String COL_NUM = "number";
//    public static final String COL_TITLE = "title";
    public static final String COL_RELEASEDATE = "releasedate";
    public static final String COL_TOONURL = "toonurl";

    private final String createQueryEpisodes
            = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_EPISODES + "("
            + COL_NUM + " INTEGER NOT NULL, "
            + ID + " INTEGER NOT NULL, "
            + COL_TITLE + " TEXT, "
            + COL_RELEASEDATE + " TEXT, "
            + COL_TOONURL + " TEXT, "
            + " FOREIGN KEY(" + ID + ")"
            + " REFERENCES " + TABLE_NAME_TOONS + "(" + ID + "), "
            + " PRIMARY KEY (" + COL_NUM + ", " + ID + "))";

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(createQueryToons);
        sqLiteDatabase.execSQL(createQueryEpisodes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    }

    private String getRightQuery(ToonsContainer.ReleaseDay releaseDay, boolean showHidden, boolean showCompleted, boolean showHiddenOnly, boolean showCompletedOnly) {
        String baseQuery = "SELECT ( SELECT COUNT(*) FROM " + TABLE_NAME_TOONS + " AS T2 WHERE T2." + ID + " <= T1." + ID + " ) AS row_Num, * " + " FROM " + TABLE_NAME_TOONS + " AS T1";

        ArrayList<String> whereQueries = new ArrayList<>();

        if(showHidden) {
            if(showHiddenOnly)
                whereQueries.add(COL_HIDE + " IS " + 1);
        }
        else whereQueries.add(COL_HIDE + " IS " + 0);

        if(showCompleted) {
            if (showCompletedOnly)
                whereQueries.add(COL_COMPLETE + " IS " + 1);
        }
        else whereQueries.add(COL_COMPLETE + " IS " + 0);

        if(releaseDay != ToonsContainer.ReleaseDay.NON && releaseDay != ToonsContainer.ReleaseDay.ALL)
            whereQueries.add("( " + COL_RELEASEDAY + " & " + (1 << releaseDay.getValue()) + " ) IS NOT " + 0);
        else if(releaseDay == ToonsContainer.ReleaseDay.NON)
            whereQueries.add("( " + COL_RELEASEDAY + " & " + (1 << releaseDay.getValue()) + " ) IS " + 0);

        StringBuilder toReturn = new StringBuilder(baseQuery);
        for(int i = 0; i < whereQueries.size(); ++i) {
            if(i == 0) toReturn.append(" WHERE ");
            else toReturn.append(" AND ");
            toReturn.append(whereQueries.get(i));
        }

        return toReturn.toString();
    }

    @SuppressLint("Range")
    public ArrayList<ToonsContainer> getAllToonsFiltered(ToonsContainer.ReleaseDay releaseDay, boolean showHidden, boolean showCompleted, boolean showHiddenOnly, boolean showCompletedOnly) {
        ArrayList<ToonsContainer> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selectAllQueryFiltered = getRightQuery(releaseDay, showHidden, showCompleted, showHiddenOnly, showCompletedOnly);
        Cursor cursor = db.rawQuery(selectAllQueryFiltered, null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                do {
                    list.add(
                            new ToonsContainer(
                                    cursor.getInt(cursor.getColumnIndex(ID)),
                                    cursor.getString(cursor.getColumnIndex(COL_TITLE)),
                                    cursor.getString(cursor.getColumnIndex(COL_TYPE)),
                                    cursor.getInt(cursor.getColumnIndex(COL_TOONID)),
                                    cursor.getInt(cursor.getColumnIndex(COL_EPIID)),
                                    cursor.getInt(cursor.getColumnIndex(COL_RELEASEDAY)),
                                    cursor.getInt(cursor.getColumnIndex(COL_HIDE)) != 0,
                                    cursor.getInt(cursor.getColumnIndex(COL_COMPLETE)) != 0,
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

    public void insertEpisodeContent(ToonsContainer toonsContainer, EpisodesContainer episodesContainer) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_NUM, episodesContainer.number);
        contentValues.put(ID, toonsContainer.dbID);
        contentValues.put(COL_TITLE, episodesContainer.title);
        contentValues.put(COL_RELEASEDATE, episodesContainer.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        contentValues.put(COL_TOONURL, episodesContainer.link);

        db.insert(TABLE_NAME_EPISODES, null, contentValues);

        db.setTransactionSuccessful();
        db.endTransaction();

        db.close();
    }

    public boolean tryInsertEpisodeContent(ToonsContainer toonsContainer, EpisodesContainer episodesContainer) {
        SQLiteDatabase db = getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_NAME_EPISODES + " WHERE " + ID + " IS " + toonsContainer.dbID + " AND " + COL_NUM + " IS " + episodesContainer.number;
        @SuppressLint("Recycle") Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor != null && cursor.moveToFirst()) {
            db.close();
            return false;
        }
        db.close();
        insertEpisodeContent(toonsContainer, episodesContainer);
        return true;
    }

    @SuppressLint("Range")
    public ArrayList<EpisodesContainer> getAllEpisodes(ToonsContainer toonsContainer) {
        ArrayList<EpisodesContainer> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_EPISODES + " WHERE " + ID + " IS " + toonsContainer.dbID, null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                do {
                    EpisodesContainer container = new EpisodesContainer();
                    container.number = cursor.getInt(cursor.getColumnIndex(COL_NUM));
                    container.title = cursor.getString(cursor.getColumnIndex(COL_TITLE));
                    container.date = LocalDate.parse(cursor.getString(cursor.getColumnIndex(COL_RELEASEDATE)), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    container.link = cursor.getString(cursor.getColumnIndex(COL_TOONURL));
                    list.add(container);
                } while(cursor.moveToNext());
            }
            cursor.close();
        }

        db.close();

        Collections.reverse(list);

        return list;
    }
}

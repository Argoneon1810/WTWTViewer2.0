package ark.noah.wtwtviewer20;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class BackupRestoreDBManager implements ExecutorRunner.Callback<Pair<Integer, Boolean>>{
    public static final int BACKUP_CODE = 0;
    public static final int RESTORE_CODE = 1;
    public static final int RESTORE_OLD_CODE = 2;

    Context context;
    DBHelper dbHelper;
    String date_format;

    /**
     *
     * @param context application context
     */
    public BackupRestoreDBManager(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
        date_format = context.getString(R.string.date_format);
    }

    public boolean backup(Uri fileUri, ArrayList<ToonsContainer> tcs) {
        new ExecutorRunner().execute(() -> {
            try {
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(fileUri, "w");
                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                StringBuilder sb = getBackupXMLInStringBuilder(tcs);
                fileOutputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
                pfd.close();
            } catch (IOException e) {
                e.printStackTrace();
                return new Pair<>(BACKUP_CODE, false);
            }
            return new Pair<>(BACKUP_CODE, true);
        }, this);
        return true;
    }

    private StringBuilder getBackupXMLInStringBuilder(ArrayList<ToonsContainer> tcs) {
        StringBuilder sb = new StringBuilder();

        ArrayList<EpisodesContainer> ecs;
        sb.append("<"+DBHelper.TABLE_NAME_TOONS+">\n");
        for(ToonsContainer tc : tcs) {
            sb.append("\t<toon>\n");
            sb.append(toXMLElementString(DBHelper.ID, String.valueOf(tc.dbID), 2));
            sb.append(toXMLElementString(DBHelper.COL_TITLE, tc.toonName, 2));
            sb.append(toXMLElementString(DBHelper.COL_TYPE, tc.toonType, 2));
            sb.append(toXMLElementString(DBHelper.COL_TOONID, String.valueOf(tc.toonID), 2));
            sb.append(toXMLElementString(DBHelper.COL_EPIID, String.valueOf(tc.episodeID), 2));
            sb.append(toXMLElementString(DBHelper.COL_RELEASEDAY, String.valueOf(tc.releaseWeekdays), 2));
            sb.append(toXMLElementString(DBHelper.COL_HIDE, String.valueOf(tc.hide ? 1 : 0), 2));
            sb.append(toXMLElementString(DBHelper.COL_COMPLETE, String.valueOf(tc.completed ? 1 : 0), 2));
            sb.append(toXMLElementString(DBHelper.COL_THUMBNAILURL, tc.thumbnailURL, 2));

            ecs = dbHelper.getAllEpisodes(tc);
            sb.append("\t\t<"+DBHelper.TABLE_NAME_EPISODES+">\n");
            for(EpisodesContainer ec : ecs) {
                sb.append("\t\t\t<episode>\n");
                sb.append(toXMLElementString(DBHelper.COL_NUM, String.valueOf(ec.number), 4));
                sb.append(toXMLElementString(DBHelper.ID, String.valueOf(ec.dbIDofToon), 4));
                sb.append(toXMLElementString(DBHelper.COL_TITLE, String.valueOf(ec.title), 4));
                sb.append(toXMLElementString(DBHelper.COL_RELEASEDATE, ec.date.format(DateTimeFormatter.ofPattern(date_format)), 4));
                sb.append(toXMLElementString(DBHelper.COL_TOONURL, ec.link, 4));
                sb.append("\t\t\t</episode>\n");
            }
            sb.append("\t\t</"+DBHelper.TABLE_NAME_EPISODES+">\n");

            sb.append("\t</toon>\n");

            ecs.clear();
        }
        sb.append("</"+DBHelper.TABLE_NAME_TOONS+">\n");

        return sb;
    }

    private String toXMLElementString(String nodeName, String value, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indentLevel; ++i) sb.append("\t");
        sb.append("<").append(nodeName).append(">").append(value).append("</").append(nodeName).append(">\n");
        return sb.toString();
    }

    public boolean restore(Uri fileUri) {
        new ExecutorRunner().execute(() -> {
            try {
                int maxIDInDB = dbHelper.getLastToonID();
                int startingID = (maxIDInDB == -1) ? 1 : maxIDInDB + 1;

                InputStream inputStream =
                        context.getContentResolver().openInputStream(fileUri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));

                String line;

                ArrayList<ToonsContainer> tcs = new ArrayList<>();
                ArrayList<EpisodesContainer> ecs = new ArrayList<>();

                ToonsContainer currentToonsContainer = new ToonsContainer(-1, "", "", -1, -1, -1, false, false, "");
                EpisodesContainer currentEpisodesContainer = new EpisodesContainer();
                while ((line = reader.readLine()) != null) {
                    //trim tabs for ease of filtering
                    line = line.replace("\t", "");

                    if(currentToonsContainer.isDebug) Log.i("DebugLog", line);

                    //lines for toon
                    if(line.startsWith("<"+DBHelper.ID+">") && currentToonsContainer.dbID == -1)
                        currentToonsContainer.dbID = startingID++;
                    else if(line.startsWith("<"+DBHelper.COL_TITLE+">") && currentToonsContainer.toonName.equals(""))
                        currentToonsContainer.toonName = getTextValueFromXMLLine(line);
                    else if(line.startsWith("<"+DBHelper.COL_TYPE+">"))
                        currentToonsContainer.toonType = getTextValueFromXMLLine(line);
                    else if(line.startsWith("<"+DBHelper.COL_TOONID+">"))
                        currentToonsContainer.toonID = Integer.parseInt(getTextValueFromXMLLine(line));
                    else if(line.startsWith("<"+DBHelper.COL_EPIID+">"))
                        currentToonsContainer.episodeID = Integer.parseInt(getTextValueFromXMLLine(line));
                    else if(line.startsWith("<"+DBHelper.COL_RELEASEDAY+">"))
                        currentToonsContainer.releaseWeekdays = Integer.parseInt(getTextValueFromXMLLine(line));
                    else if(line.startsWith("<"+DBHelper.COL_HIDE+">"))
                        currentToonsContainer.hide = getTextValueFromXMLLine(line).equals("1");
                    else if(line.startsWith("<"+DBHelper.COL_COMPLETE+">"))
                        currentToonsContainer.completed = getTextValueFromXMLLine(line).equals("1");
                    else if(line.startsWith("<"+DBHelper.COL_THUMBNAILURL+">"))
                        currentToonsContainer.thumbnailURL = getTextValueFromXMLLine(line);
                        //lines for episode
                    else if(line.startsWith("<"+DBHelper.COL_NUM+">"))
                        currentEpisodesContainer.number = Integer.parseInt(getTextValueFromXMLLine(line));
                    else if(line.startsWith("<"+DBHelper.ID+">") && currentToonsContainer.dbID != -1)
                        currentEpisodesContainer.dbIDofToon = currentToonsContainer.dbID;
                    else if(line.startsWith("<"+DBHelper.COL_TITLE+">") && !currentToonsContainer.toonName.equals(""))
                        currentEpisodesContainer.title = getTextValueFromXMLLine(line);
                    else if(line.startsWith("<"+DBHelper.COL_RELEASEDATE+">"))
                        currentEpisodesContainer.date = LocalDate.parse(getTextValueFromXMLLine(line), DateTimeFormatter.ofPattern(date_format));
                    else if(line.startsWith("<"+DBHelper.COL_TOONURL+">"))
                        currentEpisodesContainer.link = getTextValueFromXMLLine(line);

                    if(line.equals("</episode>")) {
                        ecs.add(currentEpisodesContainer);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "num: " + currentEpisodesContainer.number);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "dbid: " + currentEpisodesContainer.dbIDofToon);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "title: " + currentEpisodesContainer.title);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "date: " + currentEpisodesContainer.date);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "link: " + currentEpisodesContainer.link);
                        currentEpisodesContainer = new EpisodesContainer();
                    }

                    if(line.equals("</toon>")) {
                        tcs.add(currentToonsContainer);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "dbid: " + currentToonsContainer.dbID);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "title: " + currentToonsContainer.toonName);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "type: " + currentToonsContainer.toonType);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "toonid: " + currentToonsContainer.toonID);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "episodeid: " + currentToonsContainer.episodeID);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "release: " + currentToonsContainer.releaseWeekdays);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "hide: " + currentToonsContainer.hide);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "completed: " + currentToonsContainer.completed);
                        if(currentToonsContainer.isDebug) Log.i("DebugLog", "link: " + currentToonsContainer.thumbnailURL);
                        currentToonsContainer = new ToonsContainer(startingID++, "", "", -1, -1, -1, false, false, "");
                    }
                }

                for(ToonsContainer tc : tcs)
                    dbHelper.insertToonContent(tc);
                for(EpisodesContainer ec : ecs)
                    dbHelper.insertEpisodeContent(ec);
            } catch (IOException e) {
                e.printStackTrace();
                return new Pair<>(RESTORE_CODE, false);
            }
            return new Pair<>(RESTORE_CODE, true);
        }, this);
        return true;
    }

    private String getTextValueFromXMLLine(String xmlInLine) {
        return xmlInLine.substring(xmlInLine.indexOf(">") + 1, xmlInLine.lastIndexOf("<"));
    }

    public boolean restoreOld(Uri fileUri) {
        new ExecutorRunner().execute(() -> {
            try {
                int maxIDInDB = dbHelper.getLastToonID();
                int startingID = (maxIDInDB == -1) ? 1 : maxIDInDB + 1;

                InputStream is = context.getContentResolver().openInputStream(fileUri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)));
                StringBuilder lb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    lb.append(line);

                Queue<String> allSubstringsQueue = new LinkedList<>();
                allSubstringsQueue = getAllSubstrings(allSubstringsQueue, lb.toString());

                ArrayList<ToonsContainer> parsedResults = new ArrayList<>();
                String current;
                while((current = allSubstringsQueue.poll()) != null)
                    parsedResults.add(parseStringToToonsContainer(startingID++, current));
                allSubstringsQueue.clear();

                for(ToonsContainer tc : parsedResults)
                    dbHelper.insertToonContent(tc);
            } catch (IOException e) {
                e.printStackTrace();
                return new Pair<>(RESTORE_OLD_CODE, false);
            }
            return new Pair<>(RESTORE_OLD_CODE, true);
        }, this);
        return true;
    }

    private Queue<String> getAllSubstrings(Queue<String> toReturn, String value) {
        int index = value.indexOf("}");
        if(index == -1) return toReturn;
        toReturn.add(value.substring(0, index));
        return getAllSubstrings(toReturn, value.substring(index+1));
    }

    private ToonsContainer parseStringToToonsContainer(int dbid, String value) {
        int title_index = value.indexOf(DBHelper.COL_TITLE);
        int type_index = value.indexOf(DBHelper.COL_TYPE);
        int toonid_index = value.indexOf(DBHelper.COL_TOONID);
        int epiid_index = value.indexOf(DBHelper.COL_EPIID);
        int release_index = value.indexOf(DBHelper.COL_RELEASEDAY);
        int hide_index = value.indexOf(DBHelper.COL_HIDE);

        String title, type;
        int toonid, epiid, release;
        boolean hide;

        String titleSub = value.substring(title_index, type_index-1);
        titleSub = titleSub.substring(titleSub.indexOf("=")+1).trim();
        title = titleSub;

        String typeSub = value.substring(type_index, toonid_index-1);
        typeSub = typeSub.substring(typeSub.indexOf("=")+1).trim();
        type = typeSub.substring(0,2);

        String toonidSub = value.substring(toonid_index, epiid_index-1);
        toonidSub = toonidSub.substring(toonidSub.indexOf("=")+1).trim();
        toonid = Integer.parseInt(toonidSub);

        String epiidSub = value.substring(epiid_index, release_index-1);
        epiidSub = epiidSub.substring(epiidSub.indexOf("=")+1).trim();
        epiid = Integer.parseInt(epiidSub);

        String releaseSub = value.substring(release_index, hide_index-1);
        releaseSub = releaseSub.substring(releaseSub.indexOf("=")+1).trim();
        release = getCompatReleaseDays(Integer.parseInt(releaseSub));

        String hideSub = value.substring(hide_index);
        hideSub = hideSub.substring(hideSub.indexOf("=") + 1).trim();
        hide = Integer.parseInt(hideSub) != 0;

        return new ToonsContainer(dbid, title, type, toonid, epiid, release, hide, false, "");
    }

    private int getCompatReleaseDays(int flag) {
        int OLD_SUN_FLAG = 0b1000000;
        int OLD_MON_FLAG = 0b0100000;
        int OLD_TUE_FLAG = 0b0010000;
        int OLD_WED_FLAG = 0b0001000;
        int OLD_THU_FLAG = 0b0000100;
        int OLD_FRI_FLAG = 0b0000010;
        int OLD_SAT_FLAG = 0b0000001;

        int flags = 0;
        if((flag & OLD_SUN_FLAG) != 0) flags += ToonsContainer.ReleaseDay.SUN.asFlag();
        if((flag & OLD_MON_FLAG) != 0) flags += ToonsContainer.ReleaseDay.MON.asFlag();
        if((flag & OLD_TUE_FLAG) != 0) flags += ToonsContainer.ReleaseDay.TUE.asFlag();
        if((flag & OLD_WED_FLAG) != 0) flags += ToonsContainer.ReleaseDay.WED.asFlag();
        if((flag & OLD_THU_FLAG) != 0) flags += ToonsContainer.ReleaseDay.THU.asFlag();
        if((flag & OLD_FRI_FLAG) != 0) flags += ToonsContainer.ReleaseDay.FRI.asFlag();
        if((flag & OLD_SAT_FLAG) != 0) flags += ToonsContainer.ReleaseDay.SAT.asFlag();

        return flags;
    }

    @Override
    public void onComplete(Pair<Integer, Boolean> result) {
        if(result.first == BACKUP_CODE)
            if(result.second)
                Toast.makeText(context, context.getString(R.string.txt_backup_successful), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.txt_backup_failed), Toast.LENGTH_SHORT).show();
        else if(result.first == RESTORE_CODE || result.first == RESTORE_OLD_CODE)
            if(result.second)
                Toast.makeText(context, context.getString(R.string.txt_restore_successfully), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.txt_restore_failed), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(Exception e) {
        Toast.makeText(context, context.getString(R.string.txt_something_went_wrong), Toast.LENGTH_SHORT).show();
    }
}

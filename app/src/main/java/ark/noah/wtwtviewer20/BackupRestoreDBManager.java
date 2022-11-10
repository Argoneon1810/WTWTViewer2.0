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
import java.util.Objects;

public class BackupRestoreDBManager implements ExecutorRunner.Callback<Pair<Integer, Boolean>>{
    public static final int BACKUP_CODE = 0;
    public static final int RESTORE_CODE = 1;

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

    @Override
    public void onComplete(Pair<Integer, Boolean> result) {
        if(result.first == BACKUP_CODE)
            if(result.second)
                Toast.makeText(context, context.getString(R.string.txt_backup_successful), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.txt_backup_failed), Toast.LENGTH_SHORT).show();
        if(result.first == RESTORE_CODE)
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

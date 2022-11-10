package ark.noah.wtwtviewer20;

public class BackupRestoreDBManager {
    public static BackupRestoreDBManager Instance;

    public BackupRestoreDBManager() {
        Instance = Instance != null ? Instance : this;
    }


}

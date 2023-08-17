package nostalgia.framework.rom;

import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.Utils;
import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import java.util.List;
import java.util.Map;

import nostalgia.framework.ui.gamegallery.adapter.GalleryPagerAdapter;
import nostalgia.framework.data.database.GameDescription;
import nostalgia.framework.utils.NLog;

public class DownloaderDelegate {

    public DownloaderDelegate(
            GalleryPagerAdapter.OnItemClickListener listener,
            ProgressBar progressBar,
            GameDescription gameDescription) {
        this.gameDescription = gameDescription;
        this.listener = listener;
        this.progressBar = progressBar;

        task = new DownloadTask.Builder(gameDescription.url, Utils.getApp().getFilesDir())
                .setFilename(gameDescription.name + ".nes")
                .setMinIntervalMillisCallbackProcess(30) // 下载进度回调的间隔时间（毫秒）
                .setPassIfAlreadyCompleted(false) // 任务过去已完成是否要重新下载
                .setPriority(10)
                .build();
    }

    public void startDownload() {
        task.enqueue(downloadListener);
    }

    private GameDescription gameDescription;
    private GalleryPagerAdapter.OnItemClickListener listener;
    private ProgressBar progressBar;
    DownloadTask task;

    DownloadListener downloadListener = new DownloadListener() {
        Long contentLength = 0L;
        Long curBytes = 0L;

        @Override
        public void taskStart(@NonNull DownloadTask task) {
            NLog.e("test111", "taskStart ==> ");
            progressBar.setProgress(0);
        }

        @Override
        public void connectTrialStart(@NonNull DownloadTask task, @NonNull Map<String, List<String>> requestHeaderFields) {

        }

        @Override
        public void connectTrialEnd(@NonNull DownloadTask task, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {

        }

        @Override
        public void downloadFromBeginning(@NonNull DownloadTask task, @NonNull BreakpointInfo info, @NonNull ResumeFailedCause cause) {

        }

        @Override
        public void downloadFromBreakpoint(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {

        }

        @Override
        public void connectStart(@NonNull DownloadTask task, int blockIndex, @NonNull Map<String, List<String>> requestHeaderFields) {

        }

        @Override
        public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {

        }

        @Override
        public void fetchStart(@NonNull DownloadTask task, int blockIndex, long contentLength) {
            this.contentLength = contentLength;
            curBytes = 0L;
            NLog.e("test111", "contentLength ==> $contentLength");
        }

        @Override
        public void fetchProgress(@NonNull DownloadTask task, int blockIndex, long increaseBytes) {
            curBytes += increaseBytes;
            int progress = (int) (curBytes * 100 / contentLength);
            NLog.e("test111", "progress ==> $progress");
            progressBar.setProgress(progress);
            gameDescription.path = task.getFile().getAbsolutePath();
        }

        @Override
        public void fetchEnd(@NonNull DownloadTask task, int blockIndex, long contentLength) {
            listener.onItemClick(gameDescription);
            NLog.e("test111", "fetchEnd ==> $blockIndex");
        }

        @Override
        public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {
            NLog.e("test111", "EndCause ==> $cause");
            NLog.e("test111", "Exception ==> ", realCause);
        }
    };
}

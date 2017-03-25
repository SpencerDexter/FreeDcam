/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package freed.cam.apis.camera1.modules;

import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v4.provider.DocumentFile;
import freed.utils.Log;

import com.troop.freedcam.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.modules.I_RecorderStateChanged;
import freed.cam.apis.basecamera.modules.ModuleAbstract;
import freed.cam.apis.basecamera.modules.ModuleHandlerAbstract;
import freed.cam.apis.basecamera.modules.ModuleHandlerAbstract.CaptureStates;
import freed.cam.apis.camera1.CameraHolder;
import freed.utils.AppSettingsManager;
import freed.utils.PermissionHandler;

/**
 * Created by troop on 06.01.2016.
 */
public abstract class AbstractVideoModule extends ModuleAbstract implements MediaRecorder.OnInfoListener
{
    protected MediaRecorder recorder;
    protected String mediaSavePath;
    private final String TAG = AbstractVideoModule.class.getSimpleName();
    private ParcelFileDescriptor fileDescriptor;

    public AbstractVideoModule(CameraWrapperInterface cameraUiWrapper, Handler mBackgroundHandler) {
        super(cameraUiWrapper,mBackgroundHandler);
        name = cameraUiWrapper.getResString(R.string.module_video);
    }

    @Override
    public String ShortName() {
        return "Mov";
    }

    @Override
    public String LongName() {
        return "Movie";
    }

    //ModuleInterface START
    @Override
    public String ModuleName() {
        return name;
    }

    @Override
    public void DoWork()
    {
        if (!isWorking)
            startRecording();
        else
            stopRecording();

    }

    @Override
    public boolean IsWorking() {
        return isWorking;
    }
//ModuleInterface END


    protected void startRecording()
    {
        if (cameraUiWrapper.getActivityInterface().getPermissionHandler().hasRecordAudioPermission(new PermissionHandler.PermissionCallback() {
            @Override
            public void permissionGranted(boolean granted) {

            }
        })) {
            if (cameraUiWrapper.GetAppSettingsManager().getApiString(AppSettingsManager.SETTING_LOCATION).equals(cameraUiWrapper.getResString(R.string.on_)))
                cameraUiWrapper.GetCameraHolder().SetLocation(cameraUiWrapper.getActivityInterface().getLocationHandler().getCurrentLocation());
            prepareRecorder();
        }

    }

    protected void prepareRecorder()
    {
        try
        {
            Log.d(TAG, "InitMediaRecorder");
            isWorking = true;
            ((CameraHolder) cameraUiWrapper.GetCameraHolder()).GetCamera().unlock();
            recorder = initRecorder();
            recorder.setMaxFileSize(3037822976L); //~2.8 gigabyte
            recorder.setMaxDuration(7200000); //2hours
            recorder.setOnErrorListener(new OnErrorListener() {
                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    Log.e("MediaRecorder", "ErrorCode: " + what + " Extra: " + extra);
                    cameraUiWrapper.GetModuleHandler().onRecorderstateChanged(I_RecorderStateChanged.STATUS_RECORDING_STOP);
                    changeCaptureState(ModuleHandlerAbstract.CaptureStates.video_recording_stop);
                    ((CameraHolder) cameraUiWrapper.GetCameraHolder()).GetCamera().lock();
                }
            });

            mediaSavePath = cameraUiWrapper.getActivityInterface().getStorageHandler().getNewFilePath(appSettingsManager.GetWriteExternal(), ".mp4");

            setRecorderOutPutFile(mediaSavePath);
            recorder.setOnInfoListener(this);

            if (appSettingsManager.getApiString(AppSettingsManager.SETTING_OrientationHack).equals("true"))
                recorder.setOrientationHint(180);
            else
                recorder.setOrientationHint(0);

            // cameraHolder.StopPreview();
            //parameterHandler.PreviewFormat.SetValue("nv12-venus", true);

            recorder.setPreviewDisplay(((CameraHolder) cameraUiWrapper.GetCameraHolder()).getSurfaceHolder());
            // cameraHolder.StartPreview();

            try {
                Log.d(TAG,"Preparing Recorder");
                recorder.prepare();
                Log.d(TAG, "Recorder Prepared, Starting Recording");
                recorder.start();
                Log.d(TAG, "Recording started");
                sendStartToUi();

            } catch (Exception ex)
            {
                Log.e(TAG,"Recording failed");
                cameraUiWrapper.GetCameraHolder().SendUIMessage("Start Recording failed");
                Log.WriteEx(ex);
                recorder.reset();
                isWorking = false;
                ((CameraHolder) cameraUiWrapper.GetCameraHolder()).GetCamera().lock();
                recorder.release();
                isWorking = false;
                sendStopToUi();
            }
        }
        catch (NullPointerException ex)
        {
            Log.WriteEx(ex);
            cameraUiWrapper.GetCameraHolder().SendUIMessage("Start Recording failed");
            recorder.reset();
            isWorking = false;
            ((CameraHolder) cameraUiWrapper.GetCameraHolder()).GetCamera().lock();
            recorder.release();
            isWorking = false;
            sendStopToUi();

        }
    }

    private void sendStopToUi()
    {
        changeCaptureState(CaptureStates.video_recording_stop);
        cameraUiWrapper.GetModuleHandler().onRecorderstateChanged(I_RecorderStateChanged.STATUS_RECORDING_STOP);
    }

    private void sendStartToUi()
    {
        cameraUiWrapper.GetModuleHandler().onRecorderstateChanged(I_RecorderStateChanged.STATUS_RECORDING_START);
        changeCaptureState(CaptureStates.video_recording_start);
    }

    protected abstract MediaRecorder initRecorder();

    protected void stopRecording()
    {
        try {
            recorder.stop();
            Log.e(TAG, "Stop Recording");
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Stop Recording failed, was called bevor start");
            cameraUiWrapper.GetCameraHolder().SendUIMessage("Stop Recording failed, was called bevor start");
            Log.e(TAG,ex.getMessage());
            isWorking = false;
        }
        finally
        {
            recorder.reset();
            ((CameraHolder) cameraUiWrapper.GetCameraHolder()).GetCamera().lock();
            recorder.release();
            isWorking = false;
            try {
                if (VERSION.SDK_INT > VERSION_CODES.KITKAT && fileDescriptor != null) {
                    fileDescriptor.close();
                    fileDescriptor = null;
                }
            } catch (IOException e1) {
                Log.WriteEx(e1);
            }
            File file = new File(mediaSavePath);
            cameraUiWrapper.getActivityInterface().ScanFile(file);
            fireOnWorkFinish(file);
            sendStopToUi();
        }
    }

    protected void setRecorderOutPutFile(String s)
    {
        if (VERSION.SDK_INT < VERSION_CODES.KITKAT
                || !appSettingsManager.GetWriteExternal() && VERSION.SDK_INT >= VERSION_CODES.KITKAT)
            recorder.setOutputFile(s);
        else
        {
            File f = new File(s);
            DocumentFile df = cameraUiWrapper.getActivityInterface().getFreeDcamDocumentFolder();
            DocumentFile wr = df.createFile("*/*", f.getName());
            try {
                fileDescriptor = cameraUiWrapper.getContext().getContentResolver().openFileDescriptor(wr.getUri(), "rw");
                recorder.setOutputFile(fileDescriptor.getFileDescriptor());
            } catch (FileNotFoundException ex) {
                Log.WriteEx(ex);
                try {
                    fileDescriptor.close();
                } catch (IOException ex1) {
                   Log.WriteEx(ex1);
                }
            }
        }

    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
        {
            recordnextFile(mr);
        }
        else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
        {
            recordnextFile(mr);
        }
    }

    private void recordnextFile(MediaRecorder mr) {
        stopRecording();
        startRecording();
    }
}

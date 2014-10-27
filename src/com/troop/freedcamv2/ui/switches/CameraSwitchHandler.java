package com.troop.freedcamv2.ui.switches;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;


import com.troop.freedcam.R;
import com.troop.freedcamv2.camera.CameraUiWrapper;
import com.troop.freedcamv2.camera.parameters.I_ParametersLoaded;
import com.troop.freedcamv2.ui.AppSettingsManager;
import com.troop.freedcamv2.ui.MainActivity_v2;
import com.troop.freedcamv2.ui.TextureView.ExtendedSurfaceView;

/**
 * Created by troop on 20.08.2014.
 */
public class CameraSwitchHandler implements View.OnClickListener, I_ParametersLoaded
{
    CameraUiWrapper cameraUiWrapper;
    MainActivity_v2 activity;
    AppSettingsManager appSettingsManager;
    ImageView imageView;
    int currentCamera;
    Bitmap[] bitmaps;
    ExtendedSurfaceView surfaceView;
    public CameraSwitchHandler(MainActivity_v2 activity, CameraUiWrapper cameraUiWrapper, AppSettingsManager appSettingsManager, ExtendedSurfaceView surfaceView)
    {
        this.activity = activity;
        this.cameraUiWrapper = cameraUiWrapper;
        this.appSettingsManager = appSettingsManager;
        this.surfaceView = surfaceView;
        imageView = (ImageView)activity.findViewById(R.id.imageView_cameraSwitch);
        imageView.setOnClickListener(this);
        currentCamera = appSettingsManager.GetCurrentCamera();
        cameraUiWrapper.camParametersHandler.ParametersEventHandler.AddParametersLoadedListner(this);
        bitmaps = new Bitmap[3];
        Bitmap back = BitmapFactory.decodeResource(activity.getResources(), R.drawable.camera_back);
        bitmaps[0] = back;
        Bitmap front = BitmapFactory.decodeResource(activity.getResources(), R.drawable.camera_front);
        bitmaps[1] = front;
        Bitmap back3d = BitmapFactory.decodeResource(activity.getResources(), R.drawable.camera_back3d);
        bitmaps[2] = back3d;

    }

    @Override
    public void onClick(View v)
    {
        switchImageAndCamera();
    }

    private void switchImageAndCamera()
    {
        int maxcams = cameraUiWrapper.cameraHolder.CameraCout();
        if (currentCamera++ >= maxcams - 1)
            currentCamera = 0;
        imageView.setImageBitmap(bitmaps[currentCamera]);
        appSettingsManager.SetCurrentCamera(currentCamera);
        cameraUiWrapper.StopPreviewAndCamera();
        surfaceView.SwitchViewMode();
        cameraUiWrapper.StartPreviewAndCamera();

    }

    @Override
    public void ParametersLoaded() {
        imageView.setImageBitmap(bitmaps[currentCamera]);
    }
}

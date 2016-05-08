package com.freedcam.apis.camera2.camera.parameters.modes;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;

import com.freedcam.apis.camera2.camera.CameraHolderApi2;

/**
 * Created by troop on 17.03.2015.
 */
public class ControlModesApi2 extends BaseModeApi2
{

    public enum ControlModes
    {
        off,
        auto,
        SCENE_MODE,
        OFF_KEEP_STATE
    }

    public ControlModesApi2(Handler handler,CameraHolderApi2 cameraHolderApi2) {
        super(handler, cameraHolderApi2);

    }

    @Override
    public boolean IsSupported() {
        return true;
    }

    @Override
    public void SetValue(String valueToSet, boolean setToCamera) {
        ControlModes modes = Enum.valueOf(ControlModes.class, valueToSet);
        cameraHolder.SetParameterToCam(CaptureRequest.CONTROL_MODE, modes.ordinal());
    }

    @Override
    public String GetValue() {
        int i = 0;
        try {
            i = cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.CONTROL_MODE);
        }
        catch (Exception ex)
        {

        }

        ControlModes sceneModes = ControlModes.values()[i];
        return sceneModes.toString();
    }

    @Override
    public String[] GetValues()
    {
        int device = cameraHolder.characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (device == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
            return new String[]{"off",
                    "auto",
                    "SCENE_MODE",
                    "OFF_KEEP_STATE"};
        }
        else if (device == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
            return new String[]{"auto", "SCENE_MODE"};

        return super.GetValues();
    }
}
package freed.jni;

import android.location.Location;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Date;

import freed.dng.DngProfile;
import freed.utils.Log;
import freed.utils.StorageFileHandler;
import freed.utils.StringUtils;

/**
 * Created by troop on 15.02.2015.
 */
public class RawToDng
{
    static
    {
        System.loadLibrary("freedcam");
    }

    private final String TAG = RawToDng.class.getSimpleName();

    private ByteBuffer jniHandler;

    private String wbct;
    private byte[] opcode2;
    private byte[] opcode3;
    private double Altitude, Latitude,Longitude;
    private String Provider;
    private long gpsTime;
    private float[] colorMatrix1, colorMatrix2, neutralColor, fowardMatrix1, fowardMatrix2, reductionMatrix1, reductionMatrix2;
    private double[] noise;
    private int blacklevel, whitelevel,rowSize,tight, width, height;
    private String bayerformat;

    private native ByteBuffer init();
    private native void recycle(ByteBuffer jniHandler);

    private native long GetRawBytesSize(ByteBuffer jniHandler);
    private native int GetRawHeight(ByteBuffer jniHandler);
    private native void SetGPSData(double Altitude,float[] Latitude,float[] Longitude, String Provider, long gpsTime,ByteBuffer jniHandler);
    private native void SetThumbData(byte[] mThumb, int widht, int height,ByteBuffer jniHandler);
    private native void WriteDNG(ByteBuffer jniHandler);
    private native void SetOpCode3(byte[] opcode,ByteBuffer jniHandler);
    private native void SetOpCode2(byte[] opcode,ByteBuffer jniHandler);
    private native void SetRawHeight(int height,ByteBuffer jniHandler);
    private native void SetModelAndMake(String model, String make,ByteBuffer jniHandler);
    private native void SetBayerData(byte[] fileBytes, String fileout,ByteBuffer jniHandler);
    private native void SetBayerDataFD(byte[] fileBytes, int fileout, String filename,ByteBuffer jniHandler);
    private native void SetBayerInfo(float[] colorMatrix1,
                                     float[] colorMatrix2,
                                     float[] neutralColor,
                                     float[] fowardMatrix1,
                                     float[] fowardMatrix2,
                                     float[] reductionMatrix1,
                                     float[] reductionMatrix2,
                                     double[] noiseMatrix,
                                     int blacklevel,
                                     int whitelevel,
                                     String bayerformat,
                                     int rowSize,
                                     String devicename,
                                     int rawType,int width,int height, ByteBuffer jniHandler);

    private native void SetExifData(int iso,
                                           double expo,
                                           int flash,
                                           float fNum,
                                           float focalL,
                                           String imagedescription,
                                           String orientation,
                                           float exposureIndex,
                                           ByteBuffer jniHandler);

    private native void SetDateTime(String datetime,ByteBuffer jniHandler);

    private native void SetToneCurve(float tonecurve[],ByteBuffer jniHandler);
    private native void SetHueSatMapData1(float tonecurve[],ByteBuffer jniHandler);
    private native void SetHueSatMapData2(float tonecurve[],ByteBuffer jniHandler);
    private native void SetHueSatMapDims(int[] dims,ByteBuffer jniHandler);
    private native void SetBaselineExposure(float baselineexposure,ByteBuffer jniHandler);
    private native void SetBaselineExposureOffset(float baselineexposureoffset,ByteBuffer jniHandler);


    public static RawToDng GetInstance()
    {
        return new RawToDng();
    }

    private RawToDng()
    {
        jniHandler = init();

        wbct = "";
    }


    public void setOpcode2(byte[] op2)
    {
        this.opcode2 = op2;
    }

    public void setOpcode3(byte[] op3)
    {
        opcode3 = op3;
    }

    public void SetWBCT(String wbct)
    {
        this.wbct =wbct;
    }

    private float[] getWbCtMatrix(String wbct)
    {
        int wb = Integer.parseInt(wbct) / 100;
        double r,g,b;
        double tmpcol = 0;
        //red

        if( (double) wb <= 66 )
        {
            r = 255;
            g = (double) wb -10;
            g = 99.4708025861 * Math.log(g) - 161.1195681661;
            if( (double) wb <= 19)
            {
                b = 0;
            }
            else
            {
                b = (double) wb -10;
                b = 138.5177312231 * Math.log(b) - 305.0447927307;
            }
        }
        else
        {
            r = (double) wb - 60;
            r = 329.698727446 * Math.pow(r, -0.1332047592);
            g = (double) wb -60;
            g = 288.1221695283 * Math.pow(g, -0.0755148492);
            b = 255;
        }
        Log.d(TAG, "ColorTemp=" + (double) wb + " WBCT = r:" + r + " g:" + g + " b:" + b);
        float rf,gf,bf = 0;

        rf = (float) getRGBToDouble(checkminmax((int)r))/2;
        gf = (float) getRGBToDouble(checkminmax((int)g));
        bf = (float) getRGBToDouble(checkminmax((int)b))/2;
        Log.d(TAG, "ColorTemp=" + (double) wb + " WBCT = r:" +rf +" g:"+gf +" b:"+bf);
            rf = rf / gf;
            bf = bf / gf;
            gf = 1;
        Log.d(TAG, "ColorTemp=" + (double) wb + " WBCT = r:" +rf +" g:"+gf +" b:"+bf);
        return new float[]{rf, gf,bf};
    }

    private double getRGBToDouble(int color)
    {
        double t = color;
        t = t * 3 *2;
        t = t / 255;
        t = t / 3;
        t += 1;

        return t;
    }

    private int checkminmax(int val)
    {
        if (val>255)
            return 255;
        else if(val < 0)
            return 0;
        else return val;
    }




    private long GetRawSize()
    {
        if (jniHandler == null)
            return 0;
        return GetRawBytesSize(jniHandler);
    }

    public void SetGpsData(double Altitude,double Latitude,double Longitude, String Provider, long gpsTime)
    {
        if (jniHandler == null)
            return;
        this.Altitude = Altitude;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.Provider = Provider;
        this.gpsTime = gpsTime;
        Log.d(TAG,"Latitude:" + Latitude + "Longitude:" +Longitude);
        SetGPSData(Altitude, parseGpsvalue(Latitude), parseGpsvalue(Longitude), Provider, gpsTime,jniHandler);
    }

    public void setExifData(int iso,
                            double expo,
                            int flash,
                            float fNum,
                            float focalL,
                            String imagedescription,
                            String orientation,
                            float exposureIndex)
    {
        SetExifData(iso, expo, flash, fNum, focalL, imagedescription, orientation, exposureIndex,jniHandler);
        SetDateTime(StorageFileHandler.getStringExifPattern().format(new Date()),jniHandler);
        SetBaselineExposureOffset(exposureIndex,jniHandler);
    }

    private float[] parseGpsvalue(double val)
    {

        String[] sec = Location.convert(val, Location.FORMAT_SECONDS).split(":");

        double dd = Double.parseDouble(sec[0]);
        double dm = Double.parseDouble(sec[1]);
        double ds = Double.parseDouble(sec[2].replace(",","."));

        return new float[]{ (float)dd ,(float)dm,(float)ds};
    }

    public void setThumbData(byte[] mThumb, int widht, int height)
    {
        SetThumbData(mThumb, widht,height,jniHandler);
    }

    private void SetModelAndMake(String make)
    {
        SetModelAndMake(Build.MODEL, Build.MANUFACTURER,jniHandler);
    }

    public void setBayerData(byte[] fileBytes, String fileout) throws NullPointerException
    {
        if (fileBytes == null) {
            throw new NullPointerException();
        }
        SetBayerData(fileBytes, fileout,jniHandler);
        if (opcode2 != null)
            SetOpCode2(opcode2,jniHandler);
        if (opcode3 != null)
            SetOpCode3(opcode3,jniHandler);

    }

    public void SetBayerDataFD(byte[] fileBytes, ParcelFileDescriptor fileout, String filename) throws NullPointerException
    {
        if (fileBytes == null) {
            throw new NullPointerException();
        }

        SetBayerDataFD(fileBytes, fileout.getFd(), filename,jniHandler);
        if (opcode2 != null)
            SetOpCode2(opcode2,jniHandler);
        if (opcode3 != null)
            SetOpCode3(opcode3,jniHandler);
    }


    private void SetBayerInfo(float[] colorMatrix1,
                              float[] colorMatrix2,
                              float[] neutralColor,
                              float[] fowardMatrix1,
                              float[] fowardMatrix2,
                              float[] reductionMatrix1,
                              float[] reductionMatrix2,
                              double[] noise,
                              int blacklevel,
                              int whitelevel,
                              String bayerformat,
                              int rowSize,
                              int tight, int width, int height)
    {
        if (jniHandler == null)
            return;
        this.colorMatrix1 = colorMatrix1;
        this.colorMatrix2 = colorMatrix2;
        this.neutralColor = neutralColor;
        this.fowardMatrix1 = fowardMatrix1;
        this.fowardMatrix2 = fowardMatrix2;
        this.reductionMatrix1 = reductionMatrix1;
        this.reductionMatrix2 = reductionMatrix2;
        this.noise = noise;
        this.blacklevel = blacklevel;
        this.whitelevel = whitelevel;
        this.bayerformat = bayerformat;
        this.rowSize =rowSize;
        this.tight =tight;
        this.width =width;
        this.height =height;
        if (wbct == null || wbct.equals(""))
            SetBayerInfo(this.colorMatrix1, this.colorMatrix2, this.neutralColor, this.fowardMatrix1, this.fowardMatrix2, this.reductionMatrix1, this.reductionMatrix2, this.noise, this.blacklevel,this.whitelevel, this.bayerformat, this.rowSize, Build.MODEL, this.tight, this.width, this.height,this.jniHandler);
        else if (!wbct.equals(""))
            SetBayerInfo(this.colorMatrix1, this.colorMatrix2, getWbCtMatrix(wbct), this.fowardMatrix1, this.fowardMatrix2, this.reductionMatrix1, this.reductionMatrix2, this.noise, this.blacklevel,this.whitelevel, this.bayerformat, this.rowSize, Build.MODEL, this.tight, this.width, this.height,this.jniHandler);

    }


    private void setRawHeight(int height)
    {
        SetRawHeight(height,jniHandler);
    }


    public void WriteDngWithProfile(DngProfile profile)
    {
        if (profile == null)
            return;
        SetModelAndMake(Build.MANUFACTURER);
        if (profile.toneMapProfile != null)
        {
            if (profile.toneMapProfile.getToneCurve() != null)
                SetToneCurve(profile.toneMapProfile.getToneCurve(),jniHandler);
            if (profile.toneMapProfile.getHueSatMapData1() != null)
                SetHueSatMapData1(profile.toneMapProfile.getHueSatMapData1(),jniHandler);
            //SetHueSatMapData2(profile.toneMapProfile.getHueSatMapData2());
            if (profile.toneMapProfile.getHueSatMapDims() != null)
                SetHueSatMapDims(profile.toneMapProfile.getHueSatMapDims(),jniHandler);
            if (profile.toneMapProfile.getBaselineExposure() != null)
                SetBaselineExposure(profile.toneMapProfile.getBaselineExposure(),jniHandler);
            /*if (profile.toneMapProfile.getBaselineExposureOffset() != null)
                SetBaselineExposureOffset(profile);*/
        }

        SetBayerInfo(profile.matrixes.ColorMatrix1, profile.matrixes.ColorMatrix2, profile.matrixes.NeutralMatrix,
                profile.matrixes.ForwardMatrix1,profile.matrixes.ForwardMatrix2,
                profile.matrixes.ReductionMatrix1,profile.matrixes.ReductionMatrix2,profile.matrixes.NoiseReductionMatrix,profile.blacklevel, profile.whitelevel, profile.bayerPattern, profile.rowsize, profile.rawType,profile.widht,profile.height);
        WriteDNG(jniHandler);
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {

        if (jniHandler != null) {
            recycle(jniHandler);
            jniHandler = null;
        }
        super.finalize();
    }
}

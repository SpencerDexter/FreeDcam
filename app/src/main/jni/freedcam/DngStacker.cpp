//
// Created by troop on 25.10.2016.
//

#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include "../tiff/libtiff/tiffio.h"
#define  LOG_TAG    "freedcam.RawToDngNative"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

extern "C"
{
    JNIEXPORT void JNICALL Java_freed_jni_DngStack_startStack(JNIEnv *env, jobject thiz, jobjectArray filesToStack, jstring outputfile);
}

//move in pointer values to different mem region that it not get cleared on TIFFClose(tif);
void moveToMem(float * in, float *out, int count)
{
    for (int i = 0; i < count; ++i) {
        out[i] = in[i];
    }
}


JNIEXPORT void JNICALL Java_freed_jni_DngStack_startStack(JNIEnv *env, jobject thiz, jobjectArray filesToStack, jstring outputfile)
{
    int stringCount = (*env).GetArrayLength(filesToStack);
    int width,height, data10bit_length, outputcount;
    const char * files[stringCount];
    const char * outfile =(*env).GetStringUTFChars( outputfile, NULL);
    unsigned short tmpPixel;
    unsigned char * rawOutputData;
    int mergepixel;
    unsigned char* inputData;
    char * cfa;
    float* cmat1 = new float[9];
    float * cmat2 = new float[9];
    float * neutMat = new float[3];
    float * fmat1= new float[9];
    float * fmat2= new float[9];
    double *noisemat  = new double[6];
    float *blackleveltmp;
    short blacklevel;
    float * tmpmat;
    double * tmpdouble;
    short * whitelvltmp;
    short whitelvl;
    unsigned char * inbuf;
    for (int i=0; i<stringCount; i++) {
        jstring string = (jstring) (*env).GetObjectArrayElement(filesToStack, i);
        files[i] = (*env).GetStringUTFChars( string, NULL);
    }

    TIFF *tif=TIFFOpen(files[0], "rw");
    //read needed dng tags
    TIFFGetField(tif, TIFFTAG_IMAGEWIDTH, &width);
    TIFFGetField(tif, TIFFTAG_IMAGELENGTH, &height);
    TIFFGetField(tif, TIFFTAG_COLORMATRIX1, &tmpmat);
    moveToMem(tmpmat, cmat1,9);
    TIFFGetField(tif, TIFFTAG_COLORMATRIX2, &tmpmat);
    moveToMem(tmpmat, cmat2,9);
    TIFFGetField(tif, TIFFTAG_ASSHOTNEUTRAL, &tmpmat);
    moveToMem(tmpmat, neutMat,3);
    TIFFGetField(tif, TIFFTAG_FOWARDMATRIX1, &tmpmat);
    moveToMem(tmpmat, fmat1,9);
    TIFFGetField(tif, TIFFTAG_FOWARDMATRIX2, &tmpmat);
    moveToMem(tmpmat, fmat2,9);
    TIFFGetField(tif, TIFFTAG_NOISEPROFILE, &tmpdouble);
    for (int i = 0; i < 6; ++i) {
        noisemat[i] = tmpdouble[i];
    }
    TIFFGetField(tif, TIFFTAG_CFAPATTERN, &cfa);
    TIFFGetField(tif, TIFFTAG_WHITELEVEL, &whitelvltmp);
    TIFFGetField(tif, TIFFTAG_BLACKLEVEL, &blackleveltmp);
    blacklevel = blackleveltmp[0];
    whitelvl = whitelvltmp[0];
    data10bit_length = width*height*5;
    rawOutputData = new unsigned char[width*height*8];
    inputData = new unsigned char[data10bit_length];

    int scanlinesize = TIFFStripSize(tif);
    inbuf = (unsigned char*)_TIFFmalloc(scanlinesize);
    int inputPos = 0;

    for (int row = 0; row < height; row++)
    {
        TIFFReadRawStrip(tif,row, inbuf, scanlinesize);
        for (int i = 0; i < scanlinesize; ++i) {
            inputData[row*scanlinesize + i] = inbuf[i];
        }
    }
    free(inbuf);

    outputcount = 0;

    for (int i = 0; i < width*height*5; i+=5) {
        tmpPixel = (inputData[i] << 2 | (inputData[i+1] & 0b11000000) >> 6) <<6; //11111111 11
        rawOutputData[outputcount++] = tmpPixel & 0xff;
        rawOutputData[outputcount++] = tmpPixel >>8;

        tmpPixel = ((inputData[i+1] & 0b00111111 ) << 4 | (inputData[i+2] & 0b11110000) >> 4) << 6; // 222222 2222
        rawOutputData[outputcount++] = tmpPixel & 0xff;
        rawOutputData[outputcount++] = tmpPixel >>8;

        tmpPixel = ((inputData[i+2]& 0b00001111 ) << 6 | (inputData[i+3] & 0b11111100) >> 2) << 6; // 3333 333333
        rawOutputData[outputcount++] = tmpPixel & 0xff;
        rawOutputData[outputcount++] = tmpPixel >>8;

        tmpPixel = ((inputData[i+3]& 0b00000011 ) << 8 | inputData[i+4]) << 6; // 44 44444444
        rawOutputData[outputcount++] = tmpPixel & 0xff;
        rawOutputData[outputcount++] = tmpPixel >>8;
    }
    LOGD("rawOutputData written %i expected size %i 10bitdatasize %i", outputcount, width*height*16/8, data10bit_length);
    TIFFClose(tif);

    //read left dngs and merge them
    for (int i = 1; i < stringCount; ++i) {
        TIFF *tif=TIFFOpen(files[i], "rw");
        TIFFReadRawStrip(tif,0, inputData, data10bit_length);
        outputcount = 0;

        for (int i = 0; i < width*height*5; i+=5) {
            tmpPixel = (((inputData[i] << 2 | (inputData[i+1] & 0b11000000) >> 6) <<6) + (rawOutputData[outputcount] | rawOutputData[outputcount+1]<<8))/2; //11111111 11
            rawOutputData[outputcount++] = tmpPixel & 0xff;
            rawOutputData[outputcount++] = tmpPixel >>8;

            tmpPixel = ((((inputData[i+1] & 0b00111111 ) << 4 | (inputData[i+2] & 0b11110000) >> 4) << 6) + (rawOutputData[outputcount] | rawOutputData[outputcount+1]<<8))/2; // 222222 2222
            rawOutputData[outputcount++] = tmpPixel & 0xff;
            rawOutputData[outputcount++] = tmpPixel >>8;

            tmpPixel = ((((inputData[i+2]& 0b00001111 ) << 6 | (inputData[i+3] & 0b11111100) >> 2) << 6) + (rawOutputData[outputcount] | rawOutputData[outputcount+1]<<8))/2; // 3333 333333
            rawOutputData[outputcount++] = tmpPixel & 0xff;
            rawOutputData[outputcount++] = tmpPixel >>8;

            tmpPixel = ((((inputData[i+3]& 0b00000011 ) << 8 | inputData[i+4]) << 6) + (rawOutputData[outputcount] | rawOutputData[outputcount+1]<<8))/2; // 44 44444444
            rawOutputData[outputcount++] = tmpPixel & 0xff;
            rawOutputData[outputcount++] = tmpPixel >>8;
        }

        TIFFClose(tif);
    }

    //create stacked dng
    tif=TIFFOpen(outfile, "w");

    TIFFSetField (tif, TIFFTAG_SUBFILETYPE, 0);
    TIFFSetField(tif, TIFFTAG_IMAGEWIDTH, width);
    TIFFSetField(tif, TIFFTAG_IMAGELENGTH, height);
    TIFFSetField(tif, TIFFTAG_BITSPERSAMPLE, 16);
    TIFFSetField(tif, TIFFTAG_PHOTOMETRIC, PHOTOMETRIC_CFA);
    TIFFSetField(tif, TIFFTAG_COMPRESSION, COMPRESSION_NONE);
    TIFFSetField (tif, TIFFTAG_SAMPLESPERPIXEL, 1);
    TIFFSetField(tif, TIFFTAG_PLANARCONFIG, PLANARCONFIG_CONTIG);
    TIFFSetField(tif, TIFFTAG_DNGVERSION, "\001\003\0\0");
    TIFFSetField(tif, TIFFTAG_DNGBACKWARDVERSION, "\001\001\0\0");
    TIFFSetField(tif, TIFFTAG_COLORMATRIX1, 9, cmat1);
    LOGD("colormatrix1");
    TIFFSetField(tif, TIFFTAG_COLORMATRIX2, 9, cmat2);
    TIFFSetField(tif, TIFFTAG_ASSHOTNEUTRAL, 3, neutMat);
    LOGD("neutralMatrix");
    if(fmat1 != NULL)
        TIFFSetField(tif, TIFFTAG_FOWARDMATRIX1, 9,  fmat1);
    if(fmat2 != NULL)
        TIFFSetField(tif, TIFFTAG_FOWARDMATRIX2, 9,  fmat2);
    if(noisemat != NULL)
        TIFFSetField(tif, TIFFTAG_NOISEPROFILE, 6,  noisemat);
    TIFFSetField(tif, TIFFTAG_CALIBRATIONILLUMINANT1, 17);
    TIFFSetField(tif, TIFFTAG_CALIBRATIONILLUMINANT2, 21);
    TIFFSetField(tif, TIFFTAG_ORIENTATION, ORIENTATION_TOPLEFT);


    TIFFSetField (tif, TIFFTAG_CFAPATTERN, "\002\001\001\0");
    long white=whitelvl <<6;
    TIFFSetField (tif, TIFFTAG_WHITELEVEL, 1, &white);

    short CFARepeatPatternDim[] = { 2,2 };
    TIFFSetField (tif, TIFFTAG_CFAREPEATPATTERNDIM, CFARepeatPatternDim);
    int bl = blacklevel<<6;
    float *blacklevelar = new float[4];
    for (int i = 0; i < 4; ++i) {
        blacklevelar[i] = bl;
    }
    TIFFSetField (tif, TIFFTAG_BLACKLEVEL, 4, blacklevelar);
    LOGD("wrote blacklevel");
    TIFFSetField (tif, TIFFTAG_BLACKLEVELREPEATDIM, CFARepeatPatternDim);
    TIFFCheckpointDirectory(tif);

    TIFFWriteRawStrip(tif, 0, rawOutputData, width*height*2);

    TIFFRewriteDirectory(tif);

    //TIFFWriteRawStrip(tif, 0, rawOutputData, width*height);

    TIFFClose(tif);
    delete[] inputData;
    delete[] rawOutputData;
}
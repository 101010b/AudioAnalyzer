//
// Created by aladin on 13.10.2015.
//

#ifndef AUDIOANALYZER_AUDIOANALYZERHELPERJNI_H
#define AUDIOANALYZER_AUDIOANALYZERHELPERJNI_H


#include <cstdlib>
#include <cmath>
#include <jni.h>

#include "kiss_fftr.h"

typedef struct s_pixel {
    unsigned char mode;
    short f1;
    short f2;
    float scale1;
    float scale2;
} t_pixel;

class FftProcessor {
public:
    float FS;
    int LEN;
    float *WAVE;
    float *RAW;
    float *F;
    float *Y;
    float *YY;
    float *YAVG;
    float *YPEAK;
    float *ENG;
    bool noavg;
    // float *YN;
    float *WINDOW;

    float *FLAT;
    float *K1;
    float *AFILTER;
    float *BFILTER;
    float *CFILTER;

    float *FRES;
    float pkval;
    float trackf;
    int trackint0,trackint1,trackint2;

    int window;
    float amplitude_win_scaling;
    float noise_win_scaling;
    bool resetpeak;

    float pxfmin;
    float pxfmax;
    bool pxlog;
    int pxpixels;
    float *PIXELS;
    t_pixel *TPIXELS;

    kiss_fftr_cfg kiss_cfg;

public:
    FftProcessor();
    ~FftProcessor();

    float filter_A_dB(float f);
    float filter_B_dB(float f);
    float filter_C_dB(float f);

    void setData(int len, float fs, int window, float tf, short *data,float fmin, float fmax,int pixels,
                 bool logscale);
    bool process();

};


extern "C" {

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetup(JNIEnv *env, jobject obj);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetData(
        JNIEnv *env, jobject obj,
        jfloat  fs, jint window, jfloat trackf, jshortArray data,
        jfloat fmin, jfloat fmax, jint pixels, jboolean logscale);
JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorProcess(JNIEnv *env, jobject obj);
JNIEXPORT jfloatArray JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorGetData(JNIEnv *env, jobject obj,
                                                                                                       jint what, jint size);
JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorResetPeak(JNIEnv *env, jobject obj);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaveViewProcessData(JNIEnv *env, jobject obj,
                                                                                                                jintArray bmapin, int width, int height,
                                                                                                                jshortArray data, jint scale);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaterfallProcessData(JNIEnv *env, jobject obj,
                                                                                                      jintArray bmapin, int width, int height,
                                                                                                      jintArray ctab, int ctablen);


}



#endif //AUDIOANALYZER_AUDIOANALYZERHELPERJNI_H

//
// Created by aladin on 13.10.2015.
//

#ifndef AUDIOANALYZER_AUDIOANALYZERHELPERJNI_H
#define AUDIOANALYZER_AUDIOANALYZERHELPERJNI_H


#include <cstdlib>
#include <cmath>
#include <jni.h>

#include "kiss_fftr.h"

#define M_PIf 3.14159265358979323846f

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

    // float *FLAT;
    // float *K1;
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

enum FunctionGeneratorType { FG_SINE /*0*/, FG_TRI /*1*/, FG_SAW /*2*/, FG_PULSE /*3*/, FG_NOISE /*4*/ };

class FunctionGenerator {
public:

    float fs;

    float phase;
    float dphi;

    // Parameters
    FunctionGeneratorType type;
    float f;
    float gain;
    float pwm;

    bool online;

public:
    FunctionGenerator();
    FunctionGenerator(int _fs);
    ~FunctionGenerator();

    void init(int _fs);

    void setup(FunctionGeneratorType _type, float _f, float _gain, float _pwm);
    void calculateBlock(float *inFSWEEP, float *inFM, float *inPM, float *inAM, float *inPWM, float *erg, int len);
};

enum SweepGeneratorType { SG_UP /*0*/, SG_DOWN /*1*/, SG_UPDOWN /*2*/};

class SweepGenerator {
public:
    int fs;
    SweepGeneratorType type;

    float phase;

    float start;
    float stop;
    bool logsweep;
    bool loop;
    float time;

    bool online;

public:
    SweepGenerator();
    SweepGenerator(int _fs);
    ~SweepGenerator();


    void init(int _fs);
    void setup(SweepGeneratorType _type, float _start, float _stop, bool _logsweep, bool _loop);
    void trigger();
    void calculateBlock(float *erg, int len);

};


class SignalGenerator {
public:
    int fs;
    bool online;

    float *smod;
    float *sout;
    float *swp;
    int blocklen;

    bool SWEEPon;
    bool FMon;
    bool PMon;
    bool AMon;
    bool PWMon;

    FunctionGenerator *FG,*FMOD;
    SweepGenerator *SG;

public:
    SignalGenerator();
    SignalGenerator(int _fs);
    ~SignalGenerator();

    void init(int _fs);

    void setup(bool _SWEEPon, bool _FMon, bool _PMon, bool _AMon, bool _PWMon);
    void calculateBlock(short *erg, int len);
};



extern "C" {


JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetup(JNIEnv *env, jobject obj);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetData(JNIEnv *env, jobject obj,
                                                        jfloat  fs, jint window, jfloat trackf, jshortArray data,
                                                        jfloat fmin, jfloat fmax, jint pixels, jboolean logscale);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorProcess(JNIEnv *env, jobject obj);

JNIEXPORT jfloatArray JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorGetData(JNIEnv *env, jobject obj,
                                                          jint what, jint size);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorResetPeak(JNIEnv *env, jobject obj);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaveViewProcessData(JNIEnv *env, jobject obj,
                                                       jintArray bmapin, jint width, jint height, jshortArray data, jint scale);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaterfallProcessData(JNIEnv *env, jobject obj,
                                                      jintArray bmapin, jint width, jint height,
                                                      jintArray ctab, jint ctablen);


JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalSetup(JNIEnv *env, jobject obj, jint fs);
JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalProg(JNIEnv *env, jobject obj, jint param, jfloat value);
JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalSource(JNIEnv *env, jobject obj, jshortArray tgt);

JNIEXPORT jboolean JNICALL Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalWavHeader(JNIEnv *env, jobject obj, jbyteArray tgt);

}



#endif //AUDIOANALYZER_AUDIOANALYZERHELPERJNI_H

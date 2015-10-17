//
// Created by aladin on 13.10.2015.
//
#include "AudioAnalyzerHelperJNI.h"

#include <unistd.h>

FftProcessor::FftProcessor() {
    FS=0.0;
    LEN=0;
    WAVE= NULL;
    RAW= NULL;
    F= NULL;
    Y= NULL;
    YY= NULL;
    YAVG=NULL;
    YPEAK=NULL;
    PIXELS=NULL;
    ENG=NULL;
    TPIXELS=NULL;

    noavg=true;
    trackf=-1;
    trackint0=trackint1=trackint2=-1;

    // YN= NULL;
    WINDOW = NULL;
    kiss_cfg=NULL;

    K1=NULL;
    AFILTER=NULL;

    FRES=(float*)malloc(12*sizeof(float));
    pkval=1e-6;
    resetpeak=false;


    window=-1; // unset
    amplitude_win_scaling=0.0;
    noise_win_scaling=0.0;

    pxpixels=0;
    pxfmin=pxfmax=0;
    pxlog=false;


}

FftProcessor::~FftProcessor() {
    if (WAVE) free(WAVE);
    if (RAW) free(RAW);
    if (F) free(F);
    if (Y) free(Y);
    if (YY) free(YY);
    if (YAVG) free(YAVG);
    if (YPEAK) free(YPEAK);
    if (WINDOW) free(WINDOW);
    if (PIXELS) free(PIXELS);
    if (TPIXELS) free(TPIXELS);
    if (ENG) free(ENG);
    if (kiss_cfg) free(kiss_cfg);

    if (K1) free(K1);
    if (AFILTER) free(AFILTER);
}

void FftProcessor::setData(int len, float fs, int usewin, float tf, short *data,float fmin, float fmax,int pixels,
                           bool logscale) {
    bool newsetup=false;
    bool recalcwin=(usewin != window)?true:false;
    bool newfreq=false;
    bool newscale=false;

    if ((pxpixels < 1) || (pixels < 1) || (fmin != pxfmin) || (fmax != pxfmax) || (pixels != pxpixels) || (logscale != pxlog))
        newscale=true;


    if (len != LEN) {
        // New setup
        if (WAVE) free(WAVE);WAVE= NULL;
        if (RAW) free(RAW);RAW=NULL;
        if (F) free(F);F= NULL;
        if (Y) free(Y);Y= NULL;
        if (YY) free(YY);YY= NULL;
        if (YAVG) free(YAVG);YAVG= NULL;
        if (YPEAK) free(YPEAK);YPEAK= NULL;
        if (WINDOW) free(WINDOW);WINDOW=NULL;
        if (kiss_cfg) free(kiss_cfg);kiss_cfg=NULL;
        if (K1) free(K1);K1=NULL;
        if (AFILTER) free(AFILTER);AFILTER=NULL;
        if (ENG) free(ENG);ENG=NULL;

        WAVE=(float*)malloc(sizeof(float)*len);
        RAW=(float*)malloc(sizeof(float)*2*len);
        F=(float*)malloc(sizeof(float)*len/2);
        Y=(float*)malloc(sizeof(float)*len/2);
        YY=(float*)malloc(sizeof(float)*len/2);
        YAVG=(float*)malloc(sizeof(float)*len/2);
        YPEAK=(float*)malloc(sizeof(float)*len/2);
        noavg=true;
        WINDOW=(float*)malloc(sizeof(float)*len);
        K1=(float*)malloc(sizeof(float)*len/2);
        AFILTER=(float*)malloc(sizeof(float)*len/2);
        ENG=(float*)malloc(sizeof(float)*len/2);
        LEN=len;
        newsetup=true;
        recalcwin=true;
        newscale=true;
        kiss_cfg=kiss_fftr_alloc(LEN,0,0,0);
    }
    if (recalcwin) {
        float alpha,beta,a0,a1,a2;
        window = usewin;
        switch (window) {
            case 0: // Rectangular
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = 1.0f;
                amplitude_win_scaling=0.0; // dB
                noise_win_scaling=0.0; // dB
                break;
            case 1: // Hann
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = (float)(0.5 * (1.0 - cos(2.0 * M_PI * (float) i / (LEN - 1))));
                amplitude_win_scaling=(float)20.0f*log10(1/0.5);
                noise_win_scaling=(float)20.0f*log10(1/0.5*1/1.5)-0.7;
                break;
            case 2: // Hamming
                alpha = 0.54;
                beta = 1 - alpha;
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = (float)(alpha - beta * cos(2.0 * M_PI * (float) i / (LEN - 1)));
                amplitude_win_scaling=(float)20.0f*log10(1/0.54);
                noise_win_scaling=(float)20.0f*log10(1/0.54*1/1.36)-1.3;
                break;
            case 3: // Blackman
                alpha = 0.16;
                a0 = (float)((1.0 - alpha) / 2.0);
                a1 = 0.5;
                a2 = (float)(alpha / 2.0);
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = (float)(a0 - a1 * cos(2.0 * M_PI * (float) i / (LEN - 1)) +
                                        a2 * cos(4.0 * M_PI * (float) i / (LEN - 1)));
                amplitude_win_scaling=(float)20.0f*log10(1/0.42);
                noise_win_scaling=(float)20.0f*log10(1/0.42*1/1.73)-0.4;
                break;
        }
    }


    float pmin,pmax;
    float wval;
    wval=(float)data[0]/32768.0f;
    pmin=pmax=wval;
    WAVE[0]=wval;
    for (int i=1;i<len;i++) {
        wval=(float)data[i]/32768.0f;
        if (wval > pmax) pmax=wval;
        if (wval < pmin) pmin=wval;
        WAVE[i]=wval;
    }
    pkval=pmax;
    if (-pmin > pkval) pkval = -pmin;
    if (pkval < 1e-6) pkval=1e-6;

    // memcpy(WAVE,data,sizeof(float)*len);
    if (newsetup || fs != FS) {
        newfreq=true;
        FS=fs;
        for (int i=0;i<len/2;i++) {
            F[i]=(float)i/LEN*FS;
        }

        // Filter
        for (int i=0;i<len/2;i++) {
            float f=F[i];

            // 1k Filter
            if (f <= 950.0) K1[i]=0.0;
            else if (f < 1050.0) K1[i]=1.0;
            else K1[i]=0.0;

            // A-Filter
            float RA=(float)(12200.0*12200.0*f*f*f*f/((f*f+20.6*20.6)*(f*f+12200.0*12200.0)*sqrt(f*f+107.7*107.7)*sqrt(f*f+737.9*737.9))*1.258925412);
            AFILTER[i]=RA*RA;
        }
        newscale=true;
    }
    if (newfreq || tf != trackf) {
        trackf = tf;
        if (trackf > 0) {
            trackint1=(int)floor(trackf*LEN/FS+0.5);
            trackint0=trackint1-1;
            trackint2=trackint1+1;
            if ((trackint0 < 1) || (trackint2 >= len/2)) {
                trackf=-1;
                trackint0=trackint1=trackint2=-1;
            }
        } else {
            trackf=-1;
            trackint0=trackint1=trackint2=-1;
        }
    }
    if (newscale) {
        if (PIXELS) free(PIXELS);PIXELS=NULL;
        if (TPIXELS) free(TPIXELS);TPIXELS=NULL;
        pxpixels=pixels;
        pxfmin=fmin;
        pxfmax=fmax;
        pxlog=logscale;
        if (pixels > 0) {
            PIXELS=(float*)malloc(sizeof(float)*pixels);
            TPIXELS=(t_pixel*)malloc(sizeof(t_pixel)*pixels);
            float df=FS/LEN;
            for (int i=0;i<pixels;i++) {
                float f1,f2,fm;
                if (!pxlog) {
                    // Linear
                    f1=pxfmin+(i-0.5f)*(pxfmax-pxfmin)/pxpixels;
                    fm=pxfmin+(i)*(pxfmax-pxfmin)/pxpixels;
                    f2=pxfmin+(i+0.5f)*(pxfmax-pxfmin)/pxpixels;
                } else {
                    f1=(float)exp(log(pxfmin)+(i-0.5f)*log(pxfmax/pxfmin)/pxpixels);
                    fm=(float)exp(log(pxfmin)+(i)*log(pxfmax/pxfmin)/pxpixels);
                    f2=(float)exp(log(pxfmin)+(i+0.5f)*log(pxfmax/pxfmin)/pxpixels);
                }
                float n1=f1/df;
                float nm=fm/df;
                float n2=f2/df;
                if ((n2 < 0) || (n1 >= LEN/2)) {
                    TPIXELS[i].mode=0; // ignore
                }  else {
                    if (n2-n1 <= 1.0) {
                        int i1=(int)floor(nm);
                        int i2=i1+1;
                        if (i1 < 0) {
                            i1=0;i2=1;
                        }
                        if (i2 >= LEN/2) {
                            i2=LEN/2-1;
                            i1=i2-1;
                        }
                        TPIXELS[i].mode=1;
                        TPIXELS[i].f1=(short)i1;
                        TPIXELS[i].f2=(short)i2;
                        TPIXELS[i].scale1=1-(nm-i1);
                        TPIXELS[i].scale2=1-TPIXELS[i].scale1;
                    } else {
                        int i1=(int)floor(n1);
                        int i2=(int)ceil(n2);
                        if ((i2 < 0) || (i1 >= LEN/2)) {
                            TPIXELS[i].mode=0;
                        } else {
                            if (i1 < 0) i1=0;
                            if (i2 >= LEN/2) i2=LEN/2-1;
                            if (i1 == i2) {
                                TPIXELS[i].mode=2;
                                TPIXELS[i].f1=(short)i1;
                            } else {
                                TPIXELS[i].mode=3;
                                TPIXELS[i].f1=(short)i1;
                                TPIXELS[i].f2=(short)i2;
                                TPIXELS[i].scale1=(i2-i1)+1;
                            }
                        }
                    }
                }
            }
        }
    }
}

bool FftProcessor::process() {
    for (int i=0;i<LEN;i++)
        WAVE[i]*=WINDOW[i];

    kiss_fftr(kiss_cfg,(kiss_fft_scalar*)WAVE,(kiss_fft_cpx*)RAW);

    float sumflat=0.0;
    float sumflat10k=0.0;
    float sumflat20k=0.0;
    float sum1k=0.0;
    float sumA10_100=0.0;
    float sumA100_10k=0.0;
    float sumA10k_20k=0.0;
    float scalef=(float)pow(10.0,amplitude_win_scaling/10.0)*2.0f/(float)(LEN*LEN);
    float tc=0.05; // exp(-LEN/128.0f*FS/44100.0f)*0.05;
    float sumtrack=0.0;

    for (int i=0;i<LEN/2;i++) {
        float eng=(RAW[2*i+0]*RAW[2*i+0]+RAW[2*i+1]*RAW[2*i+1])*scalef;
        Y[i]=(float)(10.0*log10(eng));
        if (!noavg) {
            YAVG[i] = YAVG[i] * (1.0f-tc) + tc * eng;
            YY[i] = (float) (10.0 * log10(YAVG[i]));
            if (resetpeak || (Y[i]>YPEAK[i])) YPEAK[i]=Y[i];
        } else {
            YAVG[i] = eng;
            YPEAK[i]=Y[i];
        }
        if (i > 0) {
            if (F[i] < 10000)
                sumflat10k+=eng;
            else if (F[i] < 20000)
                sumflat20k+=eng;
            else sumflat+=eng;
        }
        ENG[i]=eng;
        sum1k+=K1[i]*eng;
        if (F[i] < 10) sumA10_100=sumA100_10k;
        else if (F[i] < 100) sumA10_100+=AFILTER[i]*eng;
        else if (F[i] < 10000) sumA100_10k+=AFILTER[i]*eng;
        else if (F[i] < 20000) sumA10k_20k+=AFILTER[i]*eng;
        if (trackint0 == i) sumtrack+=eng;
        if (trackint1 == i) sumtrack+=eng;
        if (trackint2 == i) sumtrack+=eng;
    }
    if (noavg) {
        memcpy(YY,Y,sizeof(float)*LEN/2);
        noavg=false;
    }
    resetpeak=false;

    if (pxpixels > 0) {
        for (int i=0;i<pxpixels;i++) {
            float PV=0.0f;
            switch (TPIXELS[i].mode) {
                case 1: PV=ENG[TPIXELS[i].f1]*TPIXELS[i].scale1+ENG[TPIXELS[i].f2]*TPIXELS[i].scale2;break;
                case 2: PV=ENG[TPIXELS[i].f1];break;
                case 3:
                    for (int j=TPIXELS[i].f1;j<=TPIXELS[i].f2;j++)
                        PV+=ENG[j];
                    PV/=TPIXELS[i].scale1;
                    break;
            }
            if (PV < 1e-12) PIXELS[i]=-120;
            else if (PV > 1) PIXELS[i]=0.0;
            else PIXELS[i]=10*log10(PV);
        }
    }

    // Window scaling
    FRES[0]=amplitude_win_scaling;
    FRES[1]=noise_win_scaling;

    FRES[2]=(float)10.0*log10(sumflat+sumflat10k+sumflat20k)-noise_win_scaling;
    FRES[3]=(float)10.0*log10(sumflat10k)-noise_win_scaling;
    FRES[4]=(float)10.0*log10(sumflat10k+sumflat20k)-noise_win_scaling;
    FRES[5]=(float)10.0*log10(sum1k)-noise_win_scaling;
    FRES[6]=(float)10.0*log10(sumA10_100+sumA100_10k+sumA10k_20k)-noise_win_scaling;
    FRES[7]=(float)10.0*log10(sumA10_100+sumA100_10k)-noise_win_scaling;
    FRES[8]=(float)10.0*log10(sumA100_10k)-noise_win_scaling;
    FRES[9]=(float)10.0*log10(sumA100_10k+sumA10k_20k)-noise_win_scaling;
    FRES[10]=(float)20.0*log10(pkval);
    if (sumtrack > 0)
        FRES[11]=(float)10.0*log10(sumtrack)-noise_win_scaling;
    else
        FRES[11]=-120.0f;
    return true;
}

FftProcessor *fftProcessor=NULL;
jfloatArray gArray=NULL;

// Wrapper
jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetup(JNIEnv *env, jobject obj) {
    if (fftProcessor) delete fftProcessor;
    fftProcessor=new FftProcessor();
    return true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetData(
        JNIEnv *env, jobject obj,
        jfloat  fs, jint usewin, jfloat trackf, jshortArray data,
        jfloat fmin, jfloat fmax,jint pixels, jboolean logscale) {
    if (!fftProcessor) return false;
    jshort *jdata=env->GetShortArrayElements(data,0);
    int len=env->GetArrayLength(data);
    fftProcessor->setData(len,fs,usewin,trackf,jdata,fmin,fmax,pixels,logscale);
    env->ReleaseShortArrayElements(data,jdata,0);
    return true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorProcess(JNIEnv *env, jobject obj) {
    if (fftProcessor && fftProcessor->process())
        return true;
    return false;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorResetPeak(JNIEnv *env, jobject obj) {
    if (!fftProcessor) return false;
    fftProcessor->resetpeak=true;
    return true;
}

jfloatArray Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorGetData(JNIEnv *env, jobject obj,
                                                                                     jint what, jint size) {
    jfloatArray result;
    if (!fftProcessor) {
        return NULL;
    }
    if (gArray==NULL) {
        result=env->NewFloatArray(4096);
        gArray=(jfloatArray)env->NewGlobalRef(result);
    } else {
        result=gArray;
    }
    // return result;

    switch (what) {
        case 0: // frequency
            // result=env->NewFloatArray(fftProcessor->LEN/2);

            //if (result)
            env->SetFloatArrayRegion(result,0,fftProcessor->LEN/2,fftProcessor->F);
            break;
        case 1: // Y
            //result=env->NewFloatArray(fftProcessor->LEN/2);
            //if (result)
            env->SetFloatArrayRegion(result,0,fftProcessor->LEN/2,fftProcessor->Y);
            break;
        case 2: // integrated values
            env->SetFloatArrayRegion(result,0,12,fftProcessor->FRES);
            break;
        case 3: // Average Spectrum
            env->SetFloatArrayRegion(result,0,fftProcessor->LEN/2,fftProcessor->YY);
            break;
        case 4: // Peak
            env->SetFloatArrayRegion(result,0,fftProcessor->LEN/2,fftProcessor->YPEAK);
            break;
        default:
            return NULL; // =env->NewFloatArray(0);
    }
    return result;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaveViewProcessData(JNIEnv *env, jobject obj,
                                                                                              jintArray bmapin, int width, int height,
                                                                                              jshortArray data, int scale)  {
    if (bmapin == NULL) return false;
    jint *bmap=env->GetIntArrayElements(bmapin,0);

//    memset(bmap,0,width*height*4);
    for (int i=0;i<width*height;i++) {
//        bmap[i]=0;
        unsigned char *s;
        if (bmap[i] > 0xFF000000) {
            s = (unsigned char *) &bmap[i];
            if (s[0] > 8) s[0] -= 8; else s[0] = 0;
            if (s[1] > 16) s[1] -= 16; else s[1] = 0;
            if (s[2] > 16) s[2] -= 16; else s[2] = 0;
            s[3] = 255;
        }
    }

    if (data != NULL) {
        jshort *jdata=env->GetShortArrayElements(data,0);
        int len=env->GetArrayLength(data);

        if (scale == 1) {
            for (int x=0;(x<len)&&(x<width);x++) {
                int y=(jdata[x]*height*scale)/65536+height/2;
                if (y < 0) y=0;
                if (y >= height) y=height-1;
                bmap[y*width+x]=0xFFFFFFFF;
            }

        } else {
            for (int x=0;(x<len)&&(x<width);x++) {
                int y=(jdata[x]*height*scale)/65536+height/2;
                if ((y >= 0) && (y < height))
                    bmap[y*width+x]=0xFFFFFFFF;
            }
        }
        env->ReleaseShortArrayElements(data,jdata,0);
    }

    env->ReleaseIntArrayElements(bmapin,bmap,0);

    //AndroidBitmap_unlockPixels(env, BitmapObject);

    return true;
}


jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaterfallProcessData(JNIEnv *env, jobject obj,
                                                                                    jintArray bmapin, int width, int height,
                                                                                    jintArray ctab, int ctablen) {
    if (!fftProcessor) return false;
    if (fftProcessor->pxpixels < 1) return false;
    if (bmapin == NULL) return false;
    if (ctab == NULL) return false;

    // Shift upwards
    jint *bmap=env->GetIntArrayElements(bmapin,0);
    memcpy(bmap,&bmap[2*width],(height-2)*width*4);

    jint *ct=env->GetIntArrayElements(ctab,0);

    int os=(height-2)*width;
    for (int i=0;i<width;i++) {
        int n=((int)fftProcessor->PIXELS[i]+120)*(ctablen-1)/120;
        if (n < 0) n=0;
        if (n >= ctablen) n=ctablen-1;
        bmap[os+i]=ct[n];
    }
    memcpy(&bmap[os+width],&bmap[os],width*4);

    env->ReleaseIntArrayElements(ctab,ct,0);

    env->ReleaseIntArrayElements(bmapin,bmap,0);

    return true;
}

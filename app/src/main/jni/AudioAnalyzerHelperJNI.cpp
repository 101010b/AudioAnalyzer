//
// Created by Thomas Buck on 13.10.2015.
//
// Provides basic processing algorithms for FFT and bitmaps for display

#include "AudioAnalyzerHelperJNI.h"

#include <unistd.h>

/* ******************************************************************************************* */
/* F F T  P R O C E S S O R                                                                    */
/* ******************************************************************************************* */

FftProcessor::FftProcessor() {
    FS=0.0;
    LEN=0;

    // Memory allocations
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
    trackf2=-1;
    trackint0=trackint1=trackint2=-1;

    // YN= NULL;
    WINDOW = NULL;
    kiss_cfg=NULL;

    AFILTER=NULL;
    BFILTER=NULL;
    CFILTER=NULL;

    FRES=(float*)malloc(21*sizeof(float));
    pkval=1e-6;
    resetpeak=false;


    window=-1; // unset
    amplitude_win_scaling=0.0;
    noise_win_scaling=0.0;

    pxpixels=0;
    pxfmin=pxfmax=0;
    pxlog=false;

    TERZn=34;
    TERZ=(tTerz*)malloc(sizeof(tTerz)*TERZn);
    TERZf=(float*)malloc(sizeof(float)*TERZn);
    TERZe=(float*)malloc(sizeof(float)*TERZn);
    TERZeraw=(float*)malloc(sizeof(float)*TERZn);
    TERZeavg=(float*)malloc(sizeof(float)*TERZn);
    TERZepeak=(float*)malloc(sizeof(float)*TERZn);

    float fscl=powf(2.0f,4.0f/12.0f);
    for (int i=0;i<TERZn;i++) {
        TERZf[i]=9.843133202f*powf(fscl,i);
        TERZeraw[i]=0.0f;
        TERZe[i]=-120.0f;
        TERZeavg[i]=-120.0f;
        TERZepeak[i]=-120.0f;

        TERZ[i].f1=9.843133202f*powf(fscl,(float)i-0.5f);
        TERZ[i].f2=9.843133202f*powf(fscl,(float)i+0.5f);
        TERZ[i].i1=0;
        TERZ[i].i2=0;
        TERZ[i].s1=0.0f;
        TERZ[i].s2=0.0f;
        TERZ[i].scale=0.0f;
    }

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
    if (TERZ) free(TERZ);
    if (TERZe) free(TERZe);
    if (TERZeraw) free(TERZeraw);
    if (TERZf) free(TERZf);
    if (TERZeavg) free(TERZeavg);
    if (TERZepeak) free(TERZepeak);


    if (AFILTER) free(AFILTER);
    if (BFILTER) free(BFILTER);
    if (CFILTER) free(CFILTER);
}

void FftProcessor::buildTerzFilters() {
    float dF=F[2]-F[1];
    for (int i=0;i<TERZn;i++) {
        float f1=TERZ[i].f1;
        float f2=TERZ[i].f2;
        int n1=(int)floorf(f1/dF+0.5f);
        int n2=(int)floorf(f2/dF+0.5f);
        float c1=(float)n1*dF;
        float c2=(float)n2*dF;
        float s1,s2,scale;
        if (n1 < 1) n1=1;
        if (n2 < 1) n2=1;
        if (n1 >= LEN/2) n1=LEN/2-1;
        if (n2 >= LEN/2) n2=LEN/2-1;
        float filt=1.0f;
        switch (TERZw) {
            case 0: // No filter
                filt=1.0f;
                break;
            case 1: // A
                filt=powf(10.0f,filter_A_dB(TERZf[i])/10.0f);
                break;
            case 2: // B
                filt=powf(10.0f,filter_B_dB(TERZf[i])/10.0f);
                break;
            case 3: // C
                filt=powf(10.0f,filter_C_dB(TERZf[i])/10.0f);
                break;
        }
        if (n2 == n1) {
            s1=(f2-f1)/dF;
            s2=0;
            n2=-1;
            scale=filt;
        } else {
            // n2 > n1
            s1=1.0f-((f1-c1)/dF+0.5f);
            s2=(f2-c2)/dF+0.5f;
            scale=filt;
        }
        TERZ[i].i1=(short)n1;
        TERZ[i].i2=(short)n2;
        TERZ[i].s1=s1;
        TERZ[i].s2=s2;
        TERZ[i].scale=scale;
    }
}

float FftProcessor::filter_A_dB(float f)
{
    if (f <= 0) return -999.0f;
    float ra = 12200.0f * 12200.0f * f * f * f * f /
                ((f * f + 20.6f * 20.6f) * sqrtf((f * f + 107.7f * 107.7f) * (f * f + 737.9f * 737.9f)) * (f * f + 12200.0f * 12200.0f));
    return 2.0f + 20.0f * log10f(ra);
}

float FftProcessor::filter_B_dB(float f)
{
    if (f <= 0) return -999.0f;
    float rb = 12200.0f * 12200.0f * f * f * f /
                ((f * f + 20.6f * 20.6f) * sqrtf(f * f + 158.2f * 158.2f) * (f * f + 12200.0f * 12200.0f));
    return 0.17f + 20.0f * log10f(rb);
}

float FftProcessor::filter_C_dB(float f)
{
    if (f <= 0) return -999.0f;
    float rc = 12200.0f * 12200.0f * f * f /
                ((f * f + 20.6f * 20.6f) * (f * f + 12200.0f * 12200.0f));
    return 0.06f + 20.0f * log10f(rc);
}

void FftProcessor::setData(int len, float fs, int usewin, float tf, short *data,float fmin, float fmax,int pixels,
                           bool logscale, int terzweight) {
    bool newsetup=false;
    bool recalcwin=(usewin != window);
    bool newfreq=false;
    bool newscale=false;
    bool newterz=false;

    if ((pxpixels < 1) || (pixels < 1) || (fmin != pxfmin) || (fmax != pxfmax) || (pixels != pxpixels) || (logscale != pxlog))
        newscale=true;

    if (TERZw != terzweight)
        newterz=true;

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
        if (AFILTER) free(AFILTER);AFILTER=NULL;
        if (BFILTER) free(BFILTER);BFILTER=NULL;
        if (CFILTER) free(CFILTER);CFILTER=NULL;
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
        AFILTER=(float*)malloc(sizeof(float)*len/2);
        BFILTER=(float*)malloc(sizeof(float)*len/2);
        CFILTER=(float*)malloc(sizeof(float)*len/2);
        ENG=(float*)malloc(sizeof(float)*len/2);
        LEN=len;
        newsetup=true;
        recalcwin=true;
        newscale=true;
        newterz=true;
        kiss_cfg=kiss_fftr_alloc(LEN,0,0,0);
    }
    if (recalcwin) {
        float alpha,beta,a0,a1,a2,a3,a4;
        window = usewin;
        switch (window) {
            default:
            case 0: // Rectangular
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = 1.0f;
                amplitude_win_scaling=0.0; // dB
                noise_win_scaling=0.0; // dB
                break;

            case 1: // Hann
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = 0.5f * (1.0f - cosf(2.0f * M_PIf * (float) i / (LEN - 1)));
                amplitude_win_scaling=20.0f*log10f(1.0f/0.5f);
                noise_win_scaling=20.0f*log10f(1.0f/0.5f*1.0f/1.5f)-0.7f;
                break;

            case 2: // Hamming
                alpha = 0.54;
                beta = 1 - alpha;
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = alpha - beta * cosf(2.0f * M_PIf * (float) i / (LEN - 1));
                amplitude_win_scaling=20.0f*log10f(1.0f/0.54f);
                noise_win_scaling=20.0f*log10f(1.0f/0.54f*1.0f/1.36f)-1.3f;
                break;

            case 3: // Blackman
                alpha = 0.16f;
                a0 = (1.0f - alpha) / 2.0f;
                a1 = 0.5f;
                a2 = alpha / 2.0f;
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = a0     - a1 * cosf(2.0f * M_PIf * (float) i / (LEN - 1))
                                       + a2 * cosf(4.0f * M_PIf * (float) i / (LEN - 1));
                amplitude_win_scaling=(float)20.0f*log10f(1.0f/0.42f);
                noise_win_scaling=20.0f*log10f(1.0f/0.42f*1.0f/1.73f)-0.4f;
                break;

            case 4: // Blackman-Harris
                a0=0.35875f;
                a1=0.48829f;
                a2=0.14128f;
                a3=0.01168f;
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = a0      - a1 * cosf(2.0f * M_PIf * (float) i / (LEN - 1))
                                        + a2 * cosf(4.0f * M_PIf * (float) i / (LEN - 1))
                                        - a3 * cosf(6.0f * M_PIf * (float) i / (LEN - 1));
                amplitude_win_scaling=12.0f-3.0f;
                noise_win_scaling=5.9f-2.8f;
                break;

            case 5: // Blackman-Nuttall
                a0=0.3635819;
                a1=0.4891775;
                a2=0.1365995;
                a3=0.0106411;
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = a0      - a1 * cosf(2.0f * M_PIf * (float) i / (LEN - 1))
                                        + a2 * cosf(4.0f * M_PIf * (float) i / (LEN - 1))
                                        - a3 * cosf(6.0f * M_PIf * (float) i / (LEN - 1));
                amplitude_win_scaling=12.0f-3.0f;
                noise_win_scaling=5.9f-2.8f;
                break;

            case 6: // Flat-Top
                a0=1.0;
                a1=1.93;
                a2=1.29;
                a3=0.388;
                a4=0.028;
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = a0      - a1 * cosf(2.0f * M_PIf * (float) i / (LEN - 1))
                                        + a2 * cosf(4.0f * M_PIf * (float) i / (LEN - 1))
                                        - a3 * cosf(6.0f * M_PIf * (float) i / (LEN - 1))
                                        + a4 * cosf(8.0f * M_PIf * (float) i / (LEN - 1));
                amplitude_win_scaling=0.0f;
                noise_win_scaling=-0.5f+6.0f;
                break;

            case 7: // Bartlett
                for (int i = 0; i < LEN; i++)
                    WINDOW[i] = 2.0f/(LEN-1)*((LEN-1)/2.0f-fabsf(i-(LEN-1)/2.0f));
                amplitude_win_scaling=7.0f;
                noise_win_scaling=-3.0f+5.2f;
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

        // Weighting Filters
        for (int i=0;i<len/2;i++) {
            AFILTER[i]=powf(10.0f,filter_A_dB(F[i])/10.0f);
            BFILTER[i]=powf(10.0f,filter_B_dB(F[i])/10.0f);
            CFILTER[i]=powf(10.0f,filter_C_dB(F[i])/10.0f);
        }

        newscale=true;
        newterz=true;
    }

    if (newterz) {
        // Terz Filters
        TERZw=terzweight;
        buildTerzFilters();
    }

    if (newfreq || tf != trackf) {
        trackf = tf;
        if (trackf > 0) {
            trackint1=(int)floor(trackf*LEN/FS+0.5);
            trackint0=trackint1-1;
            trackint2=trackint1+1;
            if (trackint0 < 1) trackint0=-1;
            if (trackint1 < 1) trackint1=-1;
            if (trackint2 < 1) trackint2=-1;
            if (trackint0 >= len/2) trackint0=-1;
            if (trackint1 >= len/2) trackint1=-1;
            if (trackint2 >= len/2) trackint2=-1;
            /*if ((trackint0 < 1) || (trackint2 >= len/2)) {
                trackf=-1;
                trackint0=trackint1=trackint2=-1;
            }*/
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

    float sumB10_100=0.0;
    float sumB100_10k=0.0;
    float sumB10k_20k=0.0;

    float sumC10_100=0.0;
    float sumC100_10k=0.0;
    float sumC10k_20k=0.0;

    float scalef=powf(10.0f,amplitude_win_scaling/10.0f)*2.0f/(float)(LEN*LEN);
    // float tc=0.05f; // exp(-LEN/128.0f*FS/44100.0f)*0.05;
    float tc=(float)exp(log(LEN)*0.904684-9.951);
    float sumtrack=0.0f;
    float sumtrack2=0.0f;

    int trackint21 = -1;
    int trackint20 = -1;
    int trackint22 = -1;
    if (trackf2 > 0) {
        trackint21 = (int) floor(trackf2 * LEN / FS + 0.5);
        trackint20 = trackint21 - 1;
        trackint22 = trackint21 + 1;
        if (trackint20 < 1) trackint20 = -1;
        if (trackint21 < 1) trackint21 = -1;
        if (trackint22 < 1) trackint22 = -1;
        if (trackint20 >= LEN / 2) trackint20 = -1;
        if (trackint21 >= LEN / 2) trackint21 = -1;
        if (trackint22 >= LEN / 2) trackint22 = -1;
    }

    for (int i=0;i<LEN/2;i++) {
        float eng=(RAW[2*i+0]*RAW[2*i+0]+RAW[2*i+1]*RAW[2*i+1])*scalef;
        Y[i]=(10.0f*log10f(eng));
        if (!noavg) {
            YAVG[i] = YAVG[i] * (1.0f-tc) + tc * eng;
            YY[i] = 10.0f * log10f(YAVG[i]);
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

        if ((F[i] > 950.0f) && (F[i] < 1050.0f))
            sum1k+=eng;

        if (F[i] < 10) { }
        else if (F[i] < 100) sumA10_100+=AFILTER[i]*eng;
        else if (F[i] < 10000) sumA100_10k+=AFILTER[i]*eng;
        else if (F[i] < 20000) sumA10k_20k+=AFILTER[i]*eng;

        if (F[i] < 10) { }
        else if (F[i] < 100) sumB10_100+=BFILTER[i]*eng;
        else if (F[i] < 10000) sumB100_10k+=BFILTER[i]*eng;
        else if (F[i] < 20000) sumB10k_20k+=BFILTER[i]*eng;

        if (F[i] < 10) { }
        else if (F[i] < 100) sumC10_100+=CFILTER[i]*eng;
        else if (F[i] < 10000) sumC100_10k+=CFILTER[i]*eng;
        else if (F[i] < 20000) sumC10k_20k+=CFILTER[i]*eng;

        if (trackint0 == i) sumtrack+=eng;
        if (trackint1 == i) sumtrack+=eng;
        if (trackint2 == i) sumtrack+=eng;

        if (trackint20 == i) sumtrack2+=eng;
        if (trackint21 == i) sumtrack2+=eng;
        if (trackint22 == i) sumtrack2+=eng;
    }

    if (noavg) {
        memcpy(YY,Y,sizeof(float)*LEN/2);
        noavg=false;
    }

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
                default: PV=0;break;
            }
            if (PV < 1e-12) PIXELS[i]=-120;
            else if (PV > 1.0f) PIXELS[i]=0.0;
            else PIXELS[i]=10.0f*log10f(PV);
        }
    }

    // Terz Filter Bank
    for (int i=0;i<TERZn;i++) {
        float e=0.0f;
        if (TERZ[i].i2==-1) {
            e=ENG[TERZ[i].i1]*TERZ[i].s1*TERZ[i].scale;
        } else {
            e=ENG[TERZ[i].i1]*TERZ[i].s1+ENG[TERZ[i].i2]*TERZ[i].s2;
            for (int j=TERZ[i].i1+1;j<TERZ[i].i2;j++)
                e+=ENG[j];
            e*=TERZ[i].scale;
        }
        TERZeraw[i]=TERZeraw[i]*(1.0f-tc) + tc*e;

        if (e < 1e-14)
            TERZe[i]=-140.0f;
        else
            TERZe[i]=10.0f*log10f(e)-noise_win_scaling;
        if (TERZeraw[i] < 1e-14)
            TERZeavg[i]=-140.0f;
        else
            TERZeavg[i]=10.0f*log10f(TERZeraw[i])-noise_win_scaling;
        if (TERZe[i]<-120.0f) TERZe[i]=-120.0f;
        if (TERZeavg[i]<-120.0f) TERZeavg[i]=-120.0f;

        if (resetpeak || (TERZe[i]>TERZepeak[i])) TERZepeak[i]=TERZe[i];
    }

    resetpeak=false;

    // Window scaling
    FRES[0]=amplitude_win_scaling;
    FRES[1]=noise_win_scaling;

    FRES[2]=10.0f*log10f(sumflat+sumflat10k+sumflat20k)-noise_win_scaling;
    FRES[3]=10.0f*log10f(sumflat10k)-noise_win_scaling;
    FRES[4]=10.0f*log10f(sumflat10k+sumflat20k)-noise_win_scaling;
    FRES[5]=10.0f*log10f(sum1k)-noise_win_scaling;

    FRES[6]=10.0f*log10f(sumA10_100+sumA100_10k+sumA10k_20k)-noise_win_scaling;
    FRES[7]=10.0f*log10f(sumA10_100+sumA100_10k)-noise_win_scaling;
    FRES[8]=10.0f*log10f(sumA100_10k)-noise_win_scaling;
    FRES[9]=10.0f*log10f(sumA100_10k+sumA10k_20k)-noise_win_scaling;

    FRES[10]=20.0f*log10f(pkval);
    if (sumtrack > 0)
        FRES[11]=10.0f*log10f(sumtrack)-noise_win_scaling;
    else
        FRES[11]=-120.0f;

    FRES[12]=10.0f*log10f(sumB10_100+sumB100_10k+sumB10k_20k)-noise_win_scaling;
    FRES[13]=10.0f*log10f(sumB10_100+sumB100_10k)-noise_win_scaling;
    FRES[14]=10.0f*log10f(sumB100_10k)-noise_win_scaling;
    FRES[15]=10.0f*log10f(sumB100_10k+sumB10k_20k)-noise_win_scaling;

    FRES[16]=10.0f*log10f(sumC10_100+sumC100_10k+sumC10k_20k)-noise_win_scaling;
    FRES[17]=10.0f*log10f(sumC10_100+sumC100_10k)-noise_win_scaling;
    FRES[18]=10.0f*log10f(sumC100_10k)-noise_win_scaling;
    FRES[19]=10.0f*log10f(sumC100_10k+sumC10k_20k)-noise_win_scaling;

    if (sumtrack2 > 0)
        FRES[20]=10.0f*log10f(sumtrack2)-noise_win_scaling;
    else
        FRES[20]=-120.0f;

    return true;
}


/* ******************************************************************************************* */
/* F U N C T I O N  G E N E R A T O R                                                          */
/* ******************************************************************************************* */
void FunctionGenerator::init(int _fs) {
    type=FG_SINE;
    fs=_fs;
    phase=0;
    f=2000.0;
    dphi=2*M_PIf*f/fs;
    pwm=0.0f;
    gain=1.0f;
    online=false;
}

FunctionGenerator::FunctionGenerator() {
    init(44100);
}

FunctionGenerator::FunctionGenerator(int _fs) {
    init(_fs);
}

FunctionGenerator::~FunctionGenerator() {
    // Nothing to do...
}

unsigned int lfsr=0x12345678;
short rndgen() {
    // 32,22,2,1 - Source: Table 3 from http://www.xilinx.com/support/documentation/application_notes/xapp052.pdf
    unsigned int bit=((lfsr >> 0) ^ (lfsr >> 10) ^ (lfsr >> 30) ^ (lfsr >> 31) ) & 1;
    lfsr >>= 1;
    if (bit) lfsr |= 0x80000000;
    return (short)((lfsr >> 8)&0xFFFF);
}

float fgen(FunctionGeneratorType tp, float phase, float pwm) {
    switch (tp) {
        case FG_SINE: return sinf(phase);
        case FG_TRI:
            if (phase < M_PIf/2) return phase/(M_PIf/2);
            if (phase < 3.0f*M_PIf/2.0f) return 2-phase/(M_PIf/2);
            return -4+phase/(M_PIf/2);
        case FG_SAW:
            if (phase < M_PIf) return phase/M_PIf;
            return -2+phase/M_PIf;
        case FG_PULSE:
            if (phase < 0) return -1.0f;
            if (phase/M_PIf-1.0f < pwm) return 1.0f;
            return -1.0f;
        case FG_NOISE:
            return (float)rndgen()/32768.0f;
        default: return 0.0f;
    }
}

void FunctionGenerator::calculateBlock(float *inFSWEEP, float *inFM, float *inPM, float *inAM, float *inPWM, float *erg, int len) {
    if (!online) {
        for (int i=0;i<len;i++) erg[i]=0.0f;
        return;
    }
    dphi=2.0f*M_PIf*f/(float)fs;

    for (int i = 0; i < len; i++) {
        float dphit = 0;
        float ft=f;
        float phs=phase;
        if (inPM) {
            phs=phs+2*M_PIf*inPM[i];
            if (phs < 0) phs+=2*M_PIf;
            if (phs >= 2*M_PIf) phs-=2*M_PIf;
        }

        if (inPWM) {
            float pwmt = pwm + inPWM[i];
            erg[i] = fgen(type, phase, pwmt);
        } else
            erg[i] = fgen(type, phase, pwm);

        if (inFSWEEP || inFM) {
            if (inFSWEEP)
                ft=inFSWEEP[i];
            if (inFM)
                ft = ft * (1 + inFM[i]);
            dphit = 2 * M_PIf * ft / fs;
        } else
            dphit=dphi;

        phase += dphit;

        if (phase < 0)
            phase += 2.0 * M_PIf;
        if (phase >= 2.0 * M_PIf)
            phase -= 2.0 * M_PIf;

        if (inAM)
            erg[i] *= (1.0f+inAM[i]);
        erg[i]*=gain;
    }
}

void FunctionGenerator::setup(FunctionGeneratorType _type, float _f, float _gain, float _pwm) {
    f=_f;
    dphi=2*M_PIf*f/fs;
    gain=_gain;
    pwm=_pwm;
    type=_type;
}

/* ******************************************************************************************* */
/* S W E E P   G E N E R A T O R                                                               */
/* ******************************************************************************************* */

void SweepGenerator::init(int _fs) {
    fs=_fs;
    type = SG_UP;
    start=0;
    stop=0;
    logsweep=false;
    loop=true;
    time=0;
    phase=0;
    online=false;
}

SweepGenerator::SweepGenerator() {
    init(44100);
}

SweepGenerator::SweepGenerator(int _fs) {
    init(_fs);
}

SweepGenerator::~SweepGenerator() {
    // Nothing to do
}

void SweepGenerator::setup(SweepGeneratorType _type, float _start, float _stop, bool _logsweep, bool _loop) {
    type=_type;
    start=_start;
    stop=_stop;
    logsweep=_logsweep;
    loop=_loop;
}

void SweepGenerator::trigger() {
    phase=0;
}

void SweepGenerator::calculateBlock(float *erg, int len) {
    if (!online) {
        float val;
        switch (type) {
            case SG_UP:
                val = phase;
                break;
            case SG_DOWN:
                val = 1.0f - phase;
                break;
            case SG_UPDOWN:
                if (phase < 0.5f) val = 2.0f * phase;
                else val = 2.0f - 2.0f * phase;
        }
        if (logsweep)
            val=start*expf(val*logf(stop/start));
        else
            val=start+val*(stop-start);
        for (int i=0;i<len;i++)
            erg[i]=val;
        return;
    }
    for (int i=0;i<len;i++) {
        float val=0.0f;
        switch (type) {
            case SG_UP:
                val = phase;
                break;
            case SG_DOWN:
                val = 1.0f - phase;
                break;
            case SG_UPDOWN:
                if (phase < 0.5f) val = 2.0f * phase;
                else val = 2.0f - 2.0f * phase;
        }
        if (phase < 1.0f) {
            phase += 1.0f / fs / time;
            if (phase >=1.0f) {
                if (loop) phase-=1.0;
            }
        }
        if (logsweep)
            erg[i]=start*expf(val*logf(stop/start));
        else
            erg[i]=start+val*(stop-start);
    }
}

/* ******************************************************************************************* */
/* S I G N A L   G E N E R A T O R                                                             */
/* ******************************************************************************************* */

void SignalGenerator::init(int _fs) {
    fs=_fs;
    if (FG) delete FG;
    if (FMOD) delete FMOD;
    if (SG) delete SG;
    if (smod) free(smod); smod=NULL;
    if (swp) free(swp); swp=NULL;
    if (sout) free(sout); sout=NULL;
    FG=new FunctionGenerator(_fs);
    FMOD=new FunctionGenerator(_fs);
    SG=new SweepGenerator(_fs);
    online=false;
    blocklen=0;
    SWEEPon=false;
    AMon=FMon=PMon=PWMon=ADD=false;
    addsine=false;
    addamp=1.0;
    addfreq=1000;
    addadd=2*M_PIf*addfreq/fs;
    addt=0;
}

SignalGenerator::SignalGenerator() {
    FG=NULL;
    FMOD=NULL;
    SG=NULL;
    smod=swp=sout=NULL;
    init(44100);
}


SignalGenerator::SignalGenerator(int _fs) {
    FG=NULL;
    FMOD=NULL;
    SG=NULL;
    smod=swp=sout=NULL;
    init(_fs);
}

SignalGenerator::~SignalGenerator() {
    if (FG) delete FG;
    if (FMOD) delete FMOD;
    if (SG) delete SG;
    if (smod) free(smod); smod=NULL;
    if (swp) free(swp); swp=NULL;
    if (sout) free(sout); sout=NULL;
}

void SignalGenerator::calculateBlock(short *erg, int len) {
    if (!online) {
        if (addsine) {
            for (int i=0;i<len;i++) {
                float v=addamp*sin(addt);
                addt+=addadd;
                if (addt > 2*M_PIf)
                    addt-=2*M_PIf;
                if (v <= -1.0f) erg[i]=-32767;
                else if (v >= 1.0f) erg[i]=32767;
                else erg[i]=(short)floorf(v*32767.0f+0.5f);
            }
        } else {
            for (int i=0;i<len;i++)
                erg[i] = 0;
        }
        return;
    }
    if (len != blocklen) {
        smod=(float*)malloc(len*sizeof(float));
        swp=(float*)malloc(len*sizeof(float));
        sout=(float*)malloc(len*sizeof(float));
        blocklen=len;
    }

    FMOD->calculateBlock(NULL,NULL,NULL,NULL,NULL,smod,len);
    SG->calculateBlock(swp,len);
    FG->calculateBlock((SWEEPon)?swp:NULL,(FMon)?smod:NULL,(PMon)?smod:NULL,
                       (AMon)?smod:NULL,(PWMon)?smod:NULL,sout,len);

    for (int i=0;i<len;i++) {
        // float v=sinf(2*M_PIf*1000.0f*i/fs);
        float v=sout[i];
        if (ADD) v+=smod[i];
        if (addsine) {
            v+=addamp*sin(addt);
            addt+=addadd;
            if (addt > 2*M_PIf)
                addt-=2*M_PIf;
        }
        if (v <= -1.0f) erg[i]=-32767;
        else if (v >= 1.0f) erg[i]=32767;
        else erg[i]=(short)floorf(v*32767.0f+0.5f);
    }
}

void SignalGenerator::setup(bool _SWEEPon, bool _FMon, bool _PMon, bool _AMon, bool _PWMon) {
    SWEEPon=_SWEEPon;
    FMon=_FMon;
    PMon=_PMon;
    AMon=_AMon;
    PWMon=_PWMon;
}


/* ******************************************************************************************* */
/* W R A P P E R                                                                               */
/* ******************************************************************************************* */

FftProcessor *fftProcessor=NULL;
jfloatArray gArray=NULL;

SignalGenerator *signalGenerator=NULL;


jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalSetup(JNIEnv *env, jobject obj, jint fs) {
    if (signalGenerator) delete signalGenerator;
    signalGenerator=new SignalGenerator(fs);
    return (jboolean) (signalGenerator!=NULL);
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalProg(JNIEnv *env, jobject obj, jint param, jfloat value) {
    if (!signalGenerator) return (jboolean) false;
    switch (param) {
        case 0: signalGenerator->FMOD->f=value;break;
        case 1: signalGenerator->FMOD->gain=value;break;
        case 2: signalGenerator->FMOD->type=(FunctionGeneratorType)((int)floorf(value+0.5f)) ;break;
        case 3: signalGenerator->FMOD->pwm=value*2.0f-1.0f;break;
        case 4: signalGenerator->FMOD->online=(value>0.5f);break;
        case 5: signalGenerator->SG->start=value ;break;
        case 6: signalGenerator->SG->stop=value ;break;
        case 7: signalGenerator->SG->logsweep=(value>0.5f);break;
        case 8: signalGenerator->SG->type=(SweepGeneratorType)((int)floorf(value+0.5f));break;
        case 9: signalGenerator->SG->time=value;break;
        case 10: signalGenerator->SG->online=(value>0.5f);break;
        case 11: signalGenerator->FG->f=value;break;
        case 12: signalGenerator->FG->gain=value;break;
        case 13: signalGenerator->FG->type=(FunctionGeneratorType)((int)floorf(value+0.5f));break;
        case 14: signalGenerator->FG->pwm=value*2.0f-1.0f ;break;
        case 15: signalGenerator->FG->online=(value>0.5f);break;
        case 16: signalGenerator->SWEEPon=(value>0.5f);break;
        case 17: signalGenerator->AMon=(value>0.5f);break;
        case 18: signalGenerator->FMon=(value>0.5f);break;
        case 19: signalGenerator->PMon=(value>0.5f);break;
        case 20: signalGenerator->PWMon=(value>0.5f);break;
        case 21: signalGenerator->online=(value>0.5f);break;
        case 22: signalGenerator->SG->loop=(value>0.5f);break;
        case 23: signalGenerator->SG->trigger();break;
        case 24: signalGenerator->ADD=(value>0.5f);break;
        case 25: signalGenerator->addsine=(value>0.5f);break;
        case 26: signalGenerator->addamp=value;break;
        case 27: signalGenerator->addfreq=value;
            signalGenerator->addadd=2*M_PIf*signalGenerator->addfreq/signalGenerator->fs;
            if (fftProcessor)
                fftProcessor->trackf2=value;
            break;
        default:
            return (jboolean)false;
    }
    return (jboolean)true;
}

// Wave File Header Generator (is very difficult within Java)
// Integers have to be stored Little Endian.
typedef uint8_t RIFFID[4];

typedef struct sRIFF_RIFF {
    RIFFID ChunkID;
    uint32_t ChunkSize;
    RIFFID Format;
} tRIFF_RIFF;

typedef struct sRIFF_fmt {
    RIFFID ChunkID;
    uint32_t ChunkSize;
    uint16_t AudioFormat;
    uint16_t NumChannels;
    uint32_t SampleRate;
    uint32_t ByteRate;
    uint16_t BlockAlign;
    uint16_t BitsPerSample;
} tRIFF_fmt;

typedef struct sRIFF_data {
    RIFFID ChunkID;
    uint32_t ChunkSize;
    // Data follows here
    uint8_t data[0];
} tRIFF_data;

typedef struct sWAVEHEADER {
    tRIFF_RIFF header;
    tRIFF_fmt format;
    tRIFF_data data;
} tWAVEHEADER;

void setRIFF(RIFFID *R, char *id) {
    uint8_t *p=(uint8_t*)R;
    int i=4;
    while (i && *id) {
        *p=(uint8_t) *id;
        p++;
        id++;
        i--;
    }
    while (i) {
        *p=' ';
        p++;
        i--;
    }
}

void setLE_int16_int8(uint8_t *tgt, uint16_t s) {
    uint8_t *t=tgt;
    *t=(uint8_t) ((uint16_t) s & 0x00FF);t++;
    *t=(uint8_t) ((uint16_t) (s>>8) & 0x00FF);
}

void setLE_int16(uint16_t *tgt, uint16_t s) {
    uint8_t *t=(uint8_t *)tgt;
    *t=(uint8_t) ((uint16_t) s & 0x00FF);t++;
    *t=(uint8_t) ((uint16_t) (s>>8) & 0x00FF);
}

void setLE_int32(uint32_t *tgt, uint32_t s) {
    uint8_t *t=(uint8_t *)tgt;
    *t=(uint8_t)((uint32_t) s & 0x000000FF);t++;
    *t=(uint8_t)((uint32_t)(s>>8) & 0x000000FF);t++;
    *t=(uint8_t)((uint32_t)(s>>16) & 0x000000FF);t++;
    *t=(uint8_t)((uint32_t)(s>>24) & 0x000000FF);
}

jint Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalWavHeader(JNIEnv *env, jobject obj, jbyteArray tgt, jint samples) {

    if (env->GetArrayLength(tgt) < sizeof(tWAVEHEADER))
        return 0;

    jbyte *jdata=env->GetByteArrayElements(tgt,0);
    tWAVEHEADER *whdr=(tWAVEHEADER *) jdata;

    setRIFF(&whdr->header.ChunkID,"RIFF");
    setLE_int32(&whdr->header.ChunkSize,sizeof(tRIFF_RIFF)-4-4 + sizeof(tRIFF_fmt)+sizeof(tRIFF_data)+samples*2);
    setRIFF(&whdr->header.Format,"WAVE");

    setRIFF(&whdr->format.ChunkID,"fmt");
    setLE_int32(&whdr->format.ChunkSize,sizeof(tRIFF_fmt)-4-4);
    setLE_int16(&whdr->format.AudioFormat,1);
    setLE_int16(&whdr->format.NumChannels,1);
    setLE_int32(&whdr->format.SampleRate,44100);
    setLE_int32(&whdr->format.ByteRate,44100*2);
    setLE_int16(&whdr->format.BlockAlign,2);
    setLE_int16(&whdr->format.BitsPerSample,16);

    setRIFF(&whdr->data.ChunkID,"data");
    setLE_int32(&whdr->data.ChunkSize,sizeof(tRIFF_data)-4-4 + (uint32_t)samples*2);

    env->ReleaseByteArrayElements(tgt,jdata,0);
    return sizeof(tWAVEHEADER);
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalWavData(JNIEnv *env, jobject obj, jshortArray src, jbyteArray tgt) {
    int asize=env->GetArrayLength(src);
    if (env->GetArrayLength(tgt) < asize*2) return (jboolean)false;

    jshort *jsrc=env->GetShortArrayElements(src,0);
    jbyte *jtgt=env->GetByteArrayElements(tgt,0);
    for (int i=0;i<asize;i++) {
        setLE_int16_int8((uint8_t*)&jtgt[i*2],(uint16_t)jsrc[i]);
    }
    env->ReleaseByteArrayElements(tgt,jtgt,0);
    env->ReleaseShortArrayElements(src,jsrc,0);

    return (jboolean)true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_SignalSource(JNIEnv *env, jobject obj, jshortArray tgt) {
    if (!signalGenerator) return (jboolean)false;
    jshort *jdata=env->GetShortArrayElements(tgt,0);
    int len=env->GetArrayLength(tgt);
    signalGenerator->calculateBlock((short*)jdata,len);
    env->ReleaseShortArrayElements(tgt,jdata,0);
    return (jboolean)true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetup(JNIEnv *env, jobject obj) {
    if (fftProcessor) delete fftProcessor;
    fftProcessor=new FftProcessor();
    return (jboolean)true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorSetData(
        JNIEnv *env, jobject obj,
        jfloat  fs, jint usewin, jfloat trackf, jshortArray data, jint len,
        jfloat fmin, jfloat fmax,jint pixels, jboolean logscale,
        jint terzw) {
    if (!fftProcessor) return (jboolean)false;
    jshort *jdata=env->GetShortArrayElements(data,0);
    // int len=env->GetArrayLength(data);
    fftProcessor->setData(len,fs,usewin,trackf,jdata,fmin,fmax,pixels,logscale,terzw);
    env->ReleaseShortArrayElements(data,jdata,0);
    return (jboolean)true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorProcess(JNIEnv *env, jobject obj) {
    return (jboolean) (fftProcessor && fftProcessor->process());
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorResetPeak(JNIEnv *env, jobject obj) {
    if (!fftProcessor) return (jboolean)false;
    fftProcessor->resetpeak=true;
    return (jboolean)true;
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
            env->SetFloatArrayRegion(result,0,21,fftProcessor->FRES);
            break;
        case 3: // Average Spectrum
            env->SetFloatArrayRegion(result,0,fftProcessor->LEN/2,fftProcessor->YY);
            break;
        case 4: // Peak
            env->SetFloatArrayRegion(result,0,fftProcessor->LEN/2,fftProcessor->YPEAK);
            break;
        case 5: // TerzF
            env->SetFloatArrayRegion(result,0,fftProcessor->TERZn,fftProcessor->TERZf);
            break;
        case 6: // TerzE
            env->SetFloatArrayRegion(result,0,fftProcessor->TERZn,fftProcessor->TERZe);
            break;
        case 7: // TerzEavg
            env->SetFloatArrayRegion(result,0,fftProcessor->TERZn,fftProcessor->TERZeavg);
            break;
        case 8: // TerzEpeak
            env->SetFloatArrayRegion(result,0,fftProcessor->TERZn,fftProcessor->TERZepeak);
            break;
        default:
            return NULL; // =env->NewFloatArray(0);
    }
    return result;
}


inline int min(int i1, int i2) { return (i1 < i2)?i1:i2; }

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_fftProcessorGetDataCopy(JNIEnv *env, jobject obj,
                                                                                      jfloatArray result, int what) {
    if (!fftProcessor) {
        return NULL;
    }

    int size=env->GetArrayLength(result);

    switch (what) {
        case 0: // frequency
            // result=env->NewFloatArray(fftProcessor->LEN/2);
            //if (result)
            env->SetFloatArrayRegion(result,0,min(fftProcessor->LEN/2,size),fftProcessor->F);
            break;
        case 1: // Y
            //result=env->NewFloatArray(fftProcessor->LEN/2);
            //if (result)
            env->SetFloatArrayRegion(result,0,min(fftProcessor->LEN/2,size),fftProcessor->Y);
            break;
        case 2: // integrated values
            env->SetFloatArrayRegion(result,0,min(21,size),fftProcessor->FRES);
            break;
        case 3: // Average Spectrum
            env->SetFloatArrayRegion(result,0,min(fftProcessor->LEN/2,size),fftProcessor->YY);
            break;
        case 4: // Peak
            env->SetFloatArrayRegion(result,0,min(fftProcessor->LEN/2,size),fftProcessor->YPEAK);
            break;
        case 5: // TerzF
            env->SetFloatArrayRegion(result,0,min(fftProcessor->TERZn,size),fftProcessor->TERZf);
            break;
        case 6: // TerzE
            env->SetFloatArrayRegion(result,0,min(fftProcessor->TERZn,size),fftProcessor->TERZe);
            break;
        case 7: // TerzEavg
            env->SetFloatArrayRegion(result,0,min(fftProcessor->TERZn,size),fftProcessor->TERZeavg);
            break;
        case 8: // TerzEpeak
            env->SetFloatArrayRegion(result,0,min(fftProcessor->TERZn,size),fftProcessor->TERZepeak);
            break;
        default:
            return (jboolean) false; // =env->NewFloatArray(0);
    }
    return (jboolean) true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaveViewProcessData(JNIEnv *env, jobject obj,
                              jintArray bmapin, int width, int height,
                              jshortArray data, int scale)  {
    if (bmapin == NULL) return (jboolean)false;
    jint *bmap=env->GetIntArrayElements(bmapin,0);


    // Decay
    for (int i=0;i<width*height;i++) {
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

    return (jboolean)true;
}

jboolean Java_com_alphadraco_audioanalyzer_AudioAnalyzerHelper_WaterfallProcessData(JNIEnv *env, jobject obj,
                            jintArray bmapin, jint width, jint height,
                            jintArray ctab, jint ctablen) {
    if (!fftProcessor) return (jboolean)false;
    if (fftProcessor->pxpixels < 1) return (jboolean)false;
    if (bmapin == NULL) return (jboolean)false;
    if (ctab == NULL) return (jboolean)false;

    // Shift upwards
    jint *bmap=env->GetIntArrayElements(bmapin,0);
    memcpy(bmap,&bmap[2*width],(size_t) (height-2)*width*4);

    jint *ct=env->GetIntArrayElements(ctab,0);

    int os=(height-2)*width;
    for (int i=0;i<width;i++) {
        int n=((int)fftProcessor->PIXELS[i]+120)*(ctablen-1)/120;
        if (n < 0) n=0;
        if (n >= ctablen) n=ctablen-1;
        bmap[os+i]=ct[n];
    }
    memcpy(&bmap[os+width],&bmap[os],(size_t)width*4);

    env->ReleaseIntArrayElements(ctab,ct,0);

    env->ReleaseIntArrayElements(bmapin,bmap,0);

    return (jboolean)true;
}

package com.alphadraco.audioanalyzer;

import java.util.Map;

/**
 * Created by aladin on 05.03.2016.
 *
 * Class to calculate a Plot Axis.
 * Based on the pixel coordinates, min and max, and text width, it creates a minor and major gird
 * and specifies to axis labels.
 *
 */

public class PlotAxis {
    // Axis Min/Max and Scaling Mode
    float aMin;
    float aMax;
    boolean logScale;

    // Axis on Screen, in Pixesl
    float pxMin;
    float pxMax;

    // Needed Spacing for Axis
    float textSpace;

    // Outputs
    int gridLength;
    int gridMemLength;
    float[] gridValues;
    float[] gridPositions;
    int[] gridStyles;

    // Consts for Style
    public final int AX_MAJOR = 0x00000001;
    public final int AX_HAS_LABEL = 0x00000002;


    void setup(float _aMin, float _aMax, boolean _logScale, float _pxMin, float _pxMax, float _textSpace) {
        aMin=_aMin;
        aMax=_aMax;
        logScale=_logScale;
        pxMin=_pxMin;
        pxMax=_pxMax;
        textSpace=_textSpace;

        gridLength=0;
        gridMemLength=100; // Preallocation
        gridValues=new float[gridMemLength];
        gridPositions=new float[gridMemLength];
        gridStyles=new int[gridMemLength];
    }

    public PlotAxis() {
        setup(0,1,false,0,1000,100);
    }

    public PlotAxis(float _aMin, float _aMax, boolean _logScale, float _pxMin, float _pxMax, float _textSpace) {
        setup(_aMin, _aMax, _logScale,_pxMin,_pxMax,_textSpace);
    }

    public PlotAxis(PlotAxis src) {
        setup(src.aMin,src.aMax,src.logScale,src.pxMin,src.pxMax,src.textSpace);
    }

    // Get pixel position for a value based on current scaling
    public float getPos(float val) {
        if (logScale) {
            if (val <= 0)
                return pxMin+(float)Math.log(0.1)*(pxMax-pxMin)/(float)Math.log(aMax/aMin);
            return pxMin+(float)Math.log(val/aMin)*(pxMax-pxMin)/(float)Math.log(aMax/aMin);
        } else {
            return pxMin+(val-aMin)*(pxMax-pxMin)/(aMax-aMin);
        }
    }

    // Internal: Add grid line - extend memory array if needed
    private void addGridPos(float val, boolean major, float px) {
        if (gridLength >= gridMemLength) {
            // Need to extend
            int newGridMemLength=gridMemLength*2;
            float[] newGridValues=new float[newGridMemLength];
            float[] newGridPositions=new float[newGridMemLength];
            int[] newGridStyles=new int[newGridMemLength];
            System.arraycopy(gridValues,0,newGridValues,0,gridLength);
            System.arraycopy(gridPositions,0,newGridPositions,0,gridLength);
            System.arraycopy(gridStyles,0,newGridStyles,0,gridLength);
            gridValues=newGridValues;
            gridPositions=newGridPositions;
            gridStyles=newGridStyles;
            gridMemLength=newGridMemLength;
        }
        gridValues[gridLength]=val;
        gridPositions[gridLength]=px;
        gridStyles[gridLength]=(major)?AX_MAJOR:0;
        gridLength++;
    }

    // Is a certain position within the displayable pxiel range?
    public boolean within(float pos) {
        if (pxMax < pxMin)
            return (pos >= pxMax) && (pos <= pxMin);
        return (pos > pxMin) && (pos <= pxMax);
    }

    // create a string for a given value - based on min and max display values
    // It uses engineering unit scaling, e.g. "k" for 1000, "M" for 1000000...
    private String getLinearString(float sMin, float sMax, float val) {
        float scaleF;
        String scaleFmt;
        float diff=sMax-sMin;
        if        (diff > 1000000000) {
            scaleF=1.0f;
            scaleFmt="%1.2e";
        } else if (diff > 100000000) {
            scaleF=1e-6f;
            scaleFmt="%1.0fM";
        } else if (diff > 10000000) {
            scaleF=1e-6f;
            scaleFmt="%1.1fM";
        } else if (diff > 1000000) {
            scaleF=1e-6f;
            scaleFmt="%1.2fM";
        } else if (diff > 100000) {
            scaleF=1e-3f;
            scaleFmt="%1.0fk";
        } else if (diff > 10000) {
            scaleF=1e-3f;
            scaleFmt="%1.1fk";
        } else if (diff > 1000) {
            scaleF=1e-3f;
            scaleFmt="%1.2fk";
        } else if (diff > 100) {
            scaleF=1.0f;
            scaleFmt="%1.0f";
        } else if (diff > 10) {
            scaleF=1.0f;
            scaleFmt="%1.1f";
        } else if (diff > 1) {
            scaleF=1.0f;
            scaleFmt="%1.2f";
        } else if (diff > 0.1) {
            scaleF=1e3f;
            scaleFmt="%1.0fm";
        } else if (diff > 0.01) {
            scaleF=1e3f;
            scaleFmt="%1.1fm";
        } else if (diff > 0.001) {
            scaleF=1e3f;
            scaleFmt="%1.2fm";
        } else if (diff > 0.0001) {
            scaleF=1e6f;
            scaleFmt="%1.0fu";
        } else if (diff > 0.00001) {
            scaleF=1e6f;
            scaleFmt="%1.1fu";
        } else if (diff > 0.000001) {
            scaleF=1e6f;
            scaleFmt="%1.2fu";
        } else if (diff > 0.0000001) {
            scaleF=1e9f;
            scaleFmt="%1.0fn";
        } else if (diff > 0.00000001) {
            scaleF=1e9f;
            scaleFmt="%1.1fn";
        } else if (diff > 0.000000001) {
            scaleF=1e9f;
            scaleFmt="%1.2fn";
        } else {
            scaleF=1.0f;
            scaleFmt="%1.2e";
        }
        return String.format(scaleFmt,val*scaleF);
    }

    // Returns a string for a value, based on current scaling
    public String getString(float val) {
        if (logScale) {
            // Logarithmic
            float ordMax=(float)Math.log10(aMax/aMin);
            if (ordMax < 2)
                return getLinearString(aMin,aMax,val);
            float ord=(float)Math.log10(val);
            return getLinearString(val/10,val*10,val);
        } else {
            // Linear
            return getLinearString(aMin,aMax,val);
        }
    }

    // recalculates the axis, based on changed pixel or limits
    public void realize() {
        if (logScale) {
            // Log Scaling
            //float order=(float)Math.ceil(Math.log10(aMax/aMin));
            float ordspace=Math.abs(getPos(aMin*10)-pxMin);
            // float ordspace=10.0f/(aMax/aMin)*(pxMax-pxMin);
            if (ordspace > pxMax-pxMin) {
                // SemiLinear Scaling
                float order=(float)Math.ceil(Math.log10(aMax-aMin));
                float scaleMaj=(float)Math.pow(10.0,order-1);
                float scaleMin=scaleMaj/10;
                float start=(float)Math.floor(aMin/scaleMin)*scaleMin;
                float stop= (float)Math.ceil(aMax/scaleMin)*scaleMin;
                float val=start;
                gridLength=0;
                while (val <= stop) {
                    boolean isMaj=false;
                    if (Math.abs((val/scaleMaj)-Math.floor(val/scaleMaj+0.5)) < 0.01) {
                        isMaj=true;
                    }
                    float pos=getPos(val); // pxMin+(float)Math.log(val/aMin)/(float)Math.log(aMax/aMin)*(pxMax-pxMin);
                    if (within(pos))
                        addGridPos(val,isMaj,pos);
                    val+=scaleMin;
                }
            } else {
                // Logarithmic Scaling
                float start=(float)Math.pow(10.0,Math.floor(Math.log10(aMin)));
                float stop=(float)Math.pow(10.0,Math.ceil(Math.log10(aMax)));
                float val=start;
                gridLength=0;
                while (val <= stop) {
                    float pos=getPos(val); // pxMin+(float)Math.log(val/aMin)/(float)Math.log(aMax/aMin)*(pxMax-pxMin);
                    if (within(pos))
                        addGridPos(val,true,pos);
                    if (ordspace > 100) {
                        // Minor gird
                        for (float scl=2.0f;scl < 10.0f;scl+=1.0f) {
                            float npos=getPos(val*scl); // pxMin+(float)Math.log(val*scl/aMin)/(float)Math.log(aMax/aMin)*(pxMax-pxMin);
                            if (within(npos))
                                addGridPos(val*scl,false,npos);
                        }
                    }
                    val*=10.0f;
                }
            }
        } else {
            // Linear Scaling
            float order=(float)Math.ceil(Math.log10(aMax-aMin));
            float scaleMaj=(float)Math.pow(10.0,order-1);
            float scaleMin=scaleMaj/10;
            float start=(float)Math.floor(aMin/scaleMin)*scaleMin;
            float stop= (float)Math.ceil(aMax/scaleMin)*scaleMin;
            float val=start;
            boolean noMinorGrid=false;
            if (Math.abs(getPos(start)-getPos(start+scaleMin)) < 20) {
                // Scale up
                noMinorGrid=true;
            }
            gridLength=0;
            while (val <= stop) {
                boolean isMaj=false;
                if (Math.abs((val/scaleMaj)-Math.floor(val/scaleMaj+0.5)) < 0.01) {
                    isMaj=true;
                }
                float pos=getPos(val);
                if (within(pos)) {
                    if ((!noMinorGrid) || isMaj)
                        addGridPos(val, isMaj, pos);
                }
                val+=scaleMin;
            }
        }

        // Check for axis labels
        // First run: Major Grid
        float lastpos = 0;
        boolean lastposvalid=false;
        for (int i=0;i<gridLength;i++) {
            if ((gridStyles[i] & 0x01) != 0) {
                // Is a major gird
                if (!lastposvalid || (Math.abs(gridPositions[i]-lastpos)) > textSpace) {
                    // Draw it
                    gridStyles[i] |= 0x02; // Draw Text
                    lastpos=gridPositions[i];
                    lastposvalid=true;
                }
            }
        }
        // Second run: Minor Grid
        for (int i=0;i<gridLength;i++) {
            int j=i-1;
            while ((j >= 0) && ((gridStyles[j] & 0x02) == 0))
                j--;
            if ((j < 0) || ((j >= 0) && (Math.abs(gridPositions[i]-gridPositions[j]) > textSpace))) {
                // Seems ok
                j=i+1;
                while ((j < gridLength) && ((gridStyles[j] & 0x02) == 0))
                    j++;
                if ((j >= gridLength) || ((j < gridLength) && (Math.abs(gridPositions[j]-gridPositions[i]) > textSpace))) {
                    // Also ok
                    gridStyles[i] |= 0x02; // Draw Text;
                }
            }
        }
    }

}

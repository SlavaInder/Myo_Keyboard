package com.example.myo_keyboard;

import static java.lang.Math.sqrt;
import static java.lang.Math.abs;

public class FeatureProcessor {
    public static float[] get_features(int[][] window) {
        // init array of features
        float[] features = new float[56];
        // init normalized window
        float[][] normalized_window = new float[8][80];

        // normalize data
        for (int i=0; i<8; i++) {
            for (int j=0; j<80; j++) {
                normalized_window[i][j] = (float) window[i][j] / 128;
            }
        }

        // get features
        float[] MAVs = get_MAVs(normalized_window);
        float[] RMSs = get_RMSs(normalized_window);
        float[] SSCs = get_SSCs(normalized_window);
        float[] WLs = get_WLs(normalized_window);
        float[] AHPs = get_AHPs(normalized_window);
        float[] MHPs = get_MHPs(normalized_window);


        // pack features
        for (int i=0; i<8; i++) {
            features[i * 7] = MAVs[i];
            features[1 + i * 7] = RMSs[i];
            features[2 + i * 7] = SSCs[i];
            features[3 + i * 7] = WLs[i];
            features[4 + i * 7] = AHPs[i];
            features[5 + i * 7] = MHPs[i];
        }

        return features;
    }


    public static float[] get_MAVs(float[][] window) {
        float[] MAVs = new float[8];

        for (int i=0; i<8; i++) {
            for (int j=0; j<80; j++) {
                MAVs[i] = MAVs[i] + window[i][j];
            }
            MAVs[i] = MAVs[i] / 80;
        }

        return MAVs;
    }


    public static float[] get_RMSs(float[][] window) {
        float[] RMSs = new float[8];

        for (int i=0; i<8; i++) {
            for (int j=0; j<80; j++) {
                RMSs[i] = RMSs[i] + window[i][j] * window[i][j];
            }
            RMSs[i] = (float) sqrt(RMSs[i] / 80);
        }

        return RMSs;
    }


    public static float[] get_SSCs(float[][] window) {
        float[] SSCs = new float[8];

        for (int i=0; i<8; i++) {
            for (int j=1; j<79; j++) {
                SSCs[i] = SSCs[i] + abs((window[i][j] - window[i][j-1]) * (window[i][j] - window[i][j+1]));
            }
        }

        return SSCs;
    }


    public static float[] get_WLs(float[][] window) {
        float[] WLs = new float[8];

        for (int i=0; i<8; i++) {
            for (int j=1; j<80; j++) {
                WLs[i] = WLs[i] + abs(window[i][j] - window[i][j-1]);
            }
        }

        return WLs;
    }


    public static float[] get_AHPs(float[][] window) {
        float[] AHPs = new float[8];

        for (int i=0; i<8; i++) {
            for (int j=0; j<80; j++) {
                AHPs[i] = AHPs[i] + window[i][j] * window[i][j];
            }
            AHPs[i] = AHPs[i] / 79;
        }

        return AHPs;
    }


    public static float[] get_MHPs(float[][] window) {
        float[] MHPs = new float[8];
        float[] temps = new float[8];

        for (int i=0; i<8; i++) {
            for (int j=0; j<79; j++) {
                MHPs[i] = MHPs[i] + (window[i][j+1] - window[i][j]) * (window[i][j+1] - window[i][j]);
            }
        }

        for (int i=0; i<8; i++) {
            for (int j=0; j<80; j++) {
                temps[i] = temps[i] + window[i][j] * window[i][j];
            }
        }

        for (int i=0; i<8;i++) {
            MHPs[i] = (float) sqrt(MHPs[i]/temps[i]);
        }

        return MHPs;
    }
}

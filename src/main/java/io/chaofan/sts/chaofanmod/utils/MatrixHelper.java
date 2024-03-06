package io.chaofan.sts.chaofanmod.utils;

import com.badlogic.gdx.math.Matrix4;

public class MatrixHelper {
    public static Matrix4 createProjMatrix(
            float imageWidth, float imageHeight,
            float screenWidth, float screenHeight,
            float leftTopX, float leftTopY,
            float rightTopX, float rightTopY,
            float leftBottomX, float leftBottomY,
            float rightBottomX, float rightBottomY) {
        float[] u = {leftTopX, rightTopX, leftBottomX, rightBottomX};
        float[] v = {leftTopY, rightTopY, leftBottomY, rightBottomY};
        //float[] u = {0, 1920, 0, 1920};
        //float[] v = {0, 0, 1080, 1080};
        float[] x = {0, screenWidth, 0, screenWidth};
        float[] y = {screenHeight, screenHeight, 0, 0};

        for (int i = 0; i < 4; i++) {
            u[i] = u[i] / imageWidth * 2 - 1;
            v[i] = (imageHeight - v[i]) / imageHeight * 2 - 1;
        }

        float[][] mat = new float[12][12];
        float[] result = new float[12];
        for (int i = 0, j = 0; i < 4; i++, j+=3) {
            mat[j][0] = x[i];
            mat[j][1] = y[i];
            mat[j][2] = 1;
            mat[j][8 + i] = -u[i];
            mat[j + 1][3] = x[i];
            mat[j + 1][4] = y[i];
            mat[j + 1][5] = 1;
            mat[j + 1][8 + i] = -v[i];
            mat[j + 2][6] = x[i];
            mat[j + 2][7] = y[i];
            mat[j + 2][8 + i] = -1;
            result[j + 2] = -1;
        }

        for (int i = 0; i < mat.length; i++) {
            if (mat[i][i] == 0) {
                for (int j = i + 1; j < mat.length; j++) {
                    if (mat[j][i] != 0) {
                        float[] tmp = mat[i];
                        mat[i] = mat[j];
                        mat[j] = tmp;
                        float tmp2 = result[i];
                        result[i] = result[j];
                        result[j] = tmp2;
                        break;
                    }
                }
            }
            for (int j = i + 1; j < mat.length; j++) {
                if (mat[j][i] == 0) {
                    continue;
                }

                float scale = mat[j][i] / mat[i][i];
                for (int k = 0; k < mat[j].length; k++) {
                    mat[j][k] = mat[j][k] - scale * mat[i][k];
                }
                result[j] = result[j] - scale * result[i];
            }
        }

        for (int i = mat.length - 1; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (mat[j][i] == 0) {
                    continue;
                }

                float scale = mat[j][i] / mat[i][i];
                for (int k = 0; k < mat[j].length; k++) {
                    mat[j][k] = mat[j][k] - scale * mat[i][k];
                }
                result[j] = result[j] - scale * result[i];
            }
        }

        for (int i = mat.length - 1; i >= 0; i--) {
            result[i] /= mat[i][i];
            mat[i][i] = 1;
        }

        return new Matrix4(new float[] {
                result[0], result[3], 0, result[6],
                result[1], result[4], 0, result[7],
                0, 0, 0, 0,
                result[2], result[5], 0, 1
        });
    }
}

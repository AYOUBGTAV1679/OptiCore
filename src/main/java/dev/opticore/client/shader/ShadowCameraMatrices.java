package dev.opticore.client.shader;

/** Dependency-free column-major camera matrix helpers for future shadow passes. */
public final class ShadowCameraMatrices {
    private ShadowCameraMatrices() {}

    /** Builds a right-handed view matrix looking from eye toward center. */
    public static float[] lookAt(double eyeX, double eyeY, double eyeZ,
                                 double centerX, double centerY, double centerZ,
                                 double upX, double upY, double upZ) {
        double[] forward = normalize(centerX-eyeX, centerY-eyeY, centerZ-eyeZ, "view direction");
        double[] up = normalize(upX, upY, upZ, "up direction");
        double[] side = normalize(cross(forward, up), "view/up cross product");
        double[] correctedUp = cross(side, forward);

        float[] result = identity();
        result[0]=(float)side[0]; result[4]=(float)side[1]; result[8]=(float)side[2];
        result[1]=(float)correctedUp[0]; result[5]=(float)correctedUp[1]; result[9]=(float)correctedUp[2];
        result[2]=(float)-forward[0]; result[6]=(float)-forward[1]; result[10]=(float)-forward[2];
        result[12]=(float)-dot(side, eyeX,eyeY,eyeZ);
        result[13]=(float)-dot(correctedUp, eyeX,eyeY,eyeZ);
        result[14]=(float)dot(forward, eyeX,eyeY,eyeZ);
        return result;
    }

    /** Builds an OpenGL-style orthographic projection matrix (clip-space Z in [-1,1]). */
    public static float[] orthographic(double left, double right, double bottom, double top,
                                       double near, double far) {
        finite(left,right,bottom,top,near,far);
        if (!(right>left) || !(top>bottom) || !(far>near)) {
            throw new IllegalArgumentException("Invalid orthographic camera bounds");
        }
        float[] m = identity();
        m[0]=(float)(2.0/(right-left));
        m[5]=(float)(2.0/(top-bottom));
        m[10]=(float)(-2.0/(far-near));
        m[12]=(float)(-(right+left)/(right-left));
        m[13]=(float)(-(top+bottom)/(top-bottom));
        m[14]=(float)(-(far+near)/(far-near));
        return m;
    }

    /** Multiplies two column-major 4x4 matrices: result = left * right. */
    public static float[] multiply(float[] left, float[] right) {
        requireMatrix(left,"left"); requireMatrix(right,"right");
        float[] out=new float[16];
        for(int column=0;column<4;column++) for(int row=0;row<4;row++)
            for(int k=0;k<4;k++) out[column*4+row]+=left[k*4+row]*right[column*4+k];
        return out;
    }

    public static float[] identity() {
        return new float[]{1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    }

    private static void requireMatrix(float[] m,String name) {
        if(m==null || m.length!=16) throw new IllegalArgumentException(name+" matrix must contain 16 values");
        for(float v:m) if(!Float.isFinite(v)) throw new IllegalArgumentException(name+" matrix is non-finite");
    }
    private static void finite(double... values) {
        for(double v:values) if(!Double.isFinite(v)) throw new IllegalArgumentException("Camera bounds must be finite");
    }
    private static double[] normalize(double x,double y,double z,String name) {
        finite(x,y,z); double length=Math.hypot(Math.hypot(x,y),z);
        if(!Double.isFinite(length)||length<1.0e-12) throw new IllegalArgumentException("Degenerate "+name);
        return new double[]{x/length,y/length,z/length};
    }
    private static double[] normalize(double[] v,String name) { return normalize(v[0],v[1],v[2],name); }
    private static double[] cross(double[] a,double[] b) {
        return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
    }
    private static double dot(double[] a,double x,double y,double z) { return a[0]*x+a[1]*y+a[2]*z; }
}

package dev.opticore.client.shader;

/** Regression checks for shadow camera matrix construction. */
public final class ShadowCameraMatricesSelfTest {
    private ShadowCameraMatricesSelfTest() {}
    public static void main(String[] args) {
        float[] view=ShadowCameraMatrices.lookAt(0,0,1, 0,0,0, 0,1,0);
        check(close(view[0],1)&&close(view[5],1)&&close(view[10],1)&&close(view[14],-1),"lookAt view");
        float[] ortho=ShadowCameraMatrices.orthographic(-1,1,-1,1,1,11);
        check(close(ortho[0],1)&&close(ortho[5],1)&&close(ortho[10],-0.2f)&&close(ortho[14],-1.2f),"orthographic projection");
        float[] combined=ShadowCameraMatrices.multiply(ortho,view);
        check(close(combined[14],-1.0f),"projection-view multiplication");
        checkMatrix(ShadowCameraMatrices.identity(),
                ShadowCameraMatrices.multiply(ShadowCameraMatrices.identity(),ShadowCameraMatrices.identity()),"identity product");
        expectFailure(()->ShadowCameraMatrices.lookAt(0,0,0,0,0,0,0,1,0),"zero view direction");
        expectFailure(()->ShadowCameraMatrices.lookAt(0,0,0,0,0,-1,0,0,-1),"parallel up vector");
        expectFailure(()->ShadowCameraMatrices.orthographic(1,1,-1,1,1,10),"zero width");
        expectFailure(()->ShadowCameraMatrices.orthographic(-1,1,-1,1,10,1),"invalid depth");
        System.out.println("ShadowCameraMatricesSelfTest: OK");
    }
    private static void check(boolean condition,String message){ if(!condition) throw new AssertionError(message); }
    private static void checkMatrix(float[] e,float[] a,String message) {
        for(int i=0;i<16;i++) if(!close(e[i],a[i])) throw new AssertionError(message+" at "+i);
    }
    private static boolean close(float a,float b){return Math.abs(a-b)<0.0001f;}
    private static void expectFailure(Runnable action,String message){
        try{action.run();throw new AssertionError("Expected failure: "+message);}
        catch(IllegalArgumentException expected){}
    }
}

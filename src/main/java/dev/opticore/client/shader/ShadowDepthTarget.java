package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/**
 * Owns a depth-only texture framebuffer suitable as a future shadow-map target.
 * No world geometry is rendered by this class.
 */
public final class ShadowDepthTarget implements AutoCloseable {
    private static final int MAX_DIMENSION=8192;
    private final int framebuffer;
    private final int depthTexture;
    private final int width;
    private final int height;
    private boolean closed;

    private ShadowDepthTarget(int framebuffer,int depthTexture,int width,int height){
        this.framebuffer=framebuffer; this.depthTexture=depthTexture;
        this.width=width; this.height=height;
    }

    public static ShadowDepthTarget create(int width,int height){
        RenderSystem.assertOnRenderThread();
        if(width<1||height<1||width>MAX_DIMENSION||height>MAX_DIMENSION
                ||(long)width*height>16_777_216L)
            throw new IllegalArgumentException("Shadow map dimensions exceed safety limits");
        int previousDraw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int fbo=0,texture=0;
        try{
            fbo=GL30.glGenFramebuffers();
            texture=GL11.glGenTextures();
            if(fbo==0||texture==0) throw new IllegalStateException("Could not allocate shadow depth resources");
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL14_TEXTURE_COMPARE_MODE(),GL11.GL_NONE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL14_DEPTH_COMPONENT24(),width,height,0,
                    GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,0L);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D,texture,0);
            GL30.glDrawBuffer(GL11.GL_NONE);
            GL30.glReadBuffer(GL11.GL_NONE);
            int status=GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if(status!=GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Shadow depth framebuffer incomplete: 0x"+Integer.toHexString(status));
            return new ShadowDepthTarget(fbo,texture,width,height);
        }catch(RuntimeException|Error failure){
            if(fbo!=0) GL30.glDeleteFramebuffers(fbo);
            if(texture!=0) GL11.glDeleteTextures(texture);
            throw failure;
        }finally{
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,previousDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previousRead);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,previousTexture);
        }
    }

    // GL14 constants are kept local to avoid depending on helper state.
    private static int GL14_TEXTURE_COMPARE_MODE(){return 0x884C;}
    private static int GL14_DEPTH_COMPONENT24(){return 0x81A6;}

    public Binding bind(){
        RenderSystem.assertOnRenderThread(); ensureOpen();
        return new Binding(framebuffer,width,height);
    }
    public void clearDepth(){
        RenderSystem.assertOnRenderThread(); ensureOpen();
        boolean oldMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        double oldClear=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        try(Binding ignored=bind()){
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDepthMask(true);
            GL11.glClearDepth(1.0);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }finally{
            GL11.glDepthMask(oldMask);
            GL11.glClearDepth(oldClear);
            if(scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    public int textureId(){RenderSystem.assertOnRenderThread();ensureOpen();return depthTexture;}
    public int framebufferId(){RenderSystem.assertOnRenderThread();ensureOpen();return framebuffer;}
    public int width(){return width;}
    public int height(){return height;}
    private void ensureOpen(){if(closed)throw new IllegalStateException("Shadow depth target is closed");}
    @Override public void close(){
        RenderSystem.assertOnRenderThread();
        if(closed)return;
        if(GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)==framebuffer
                ||GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)==framebuffer)
            throw new IllegalStateException("Cannot close shadow target while bound");
        GL30.glDeleteFramebuffers(framebuffer);
        GL11.glDeleteTextures(depthTexture);
        closed=true;
    }

    public static final class Binding implements AutoCloseable{
        private final int draw,read,x,y,w,h;
        private boolean closed;
        private Binding(int framebuffer,int width,int height){
            draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            java.nio.IntBuffer viewport=org.lwjgl.BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
            x=viewport.get(0);y=viewport.get(1);w=viewport.get(2);h=viewport.get(3);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);
            GL11.glViewport(0,0,width,height);
        }
        @Override public void close(){
            RenderSystem.assertOnRenderThread();
            if(closed)return;
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            GL11.glViewport(x,y,w,h);
            closed=true;
        }
    }
}

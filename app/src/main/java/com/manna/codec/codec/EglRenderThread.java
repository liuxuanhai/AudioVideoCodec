package com.manna.codec.codec;

import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import com.manna.codec.MediaCodecConstant;
import com.manna.codec.surface.EglManager;
import com.manna.codec.surface.EglSurfaceView;

/**
 * EglRender thread
 */
public class EglRenderThread extends Thread {

    private boolean isStart;
    private boolean isStop;

    private EglManager eglManager;

    private Surface surface;
    private EGLContext eglContext;
    private EglSurfaceView.Render surfaceRender;
    private int renderMode;
    private int width;
    private int height;
    private Object object;

    public EglRenderThread(Surface surface, EGLContext eglContext, EglSurfaceView.Render surfaceRender,
                           int renderMode, int width, int height) {
        this.surface = surface;
        this.eglContext = eglContext;
        this.surfaceRender = surfaceRender;
        this.renderMode = renderMode;
        this.width = width;
        this.height = height;
    }

    @Override
    public void run() {
        super.run();
        isStart = false;
        isStop = false;
        object = new Object();
        eglManager = new EglManager();
        eglManager.init(surface, eglContext);

        while (true) {
            if (isStop) {
                release();
                break;
            }
            if (isStart) {
                if (renderMode == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                    synchronized (object) {
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                    try {
                        Thread.sleep(1000 / 60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (MediaCodecConstant.surfaceCreate && surfaceRender != null) {
                surfaceRender.onSurfaceCreated();
                MediaCodecConstant.surfaceCreate = false;
            }
            if (MediaCodecConstant.surfaceChange && surfaceRender != null) {
                surfaceRender.onSurfaceChanged(width, height);
                MediaCodecConstant.surfaceChange = false;
            }

            if (surfaceRender != null) {
                surfaceRender.onDrawFrame();
                if (!isStart) {
                    //????????????
                    surfaceRender.onDrawFrame();
                }
            }
            //OpenGL ES?????????????????????swapBuffer,????????????FrameBuffer?????????FrameBuffer????????????
            eglManager.swapBuffer();
            isStart = true;
        }
    }

    /**
     * ????????????
     */
    private void release() {
        eglManager.release();
    }

    /**
     * ????????????
     */
    public void requestRender() {
        if (object != null) {
            synchronized (object) {
                object.notifyAll();
            }
        }
    }

    public void stopEglRender() {
        isStop = true;
        requestRender();
    }
}

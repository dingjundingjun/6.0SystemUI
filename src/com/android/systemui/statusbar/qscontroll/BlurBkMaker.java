package com.android.systemui.statusbar.qscontroll;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.DisplayMetrics;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.IWindowManager;
import android.view.Surface;
import android.view.DisplayInfo;
import android.view.Display;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.RemoteException;

import com.android.internal.util.DumpUtils;
import com.android.systemui.statusbar.utils.BitmapUtils;

/**
 * 
 * Make the blur background from screen shot.
 * 
 * </br>
 * 
 * @author hmm@dw.gdbbk.com
 *
 */
public class BlurBkMaker {

    private final static String TAG = "BlurBkMaker";

    private final static float BLUR_SIZE_FACTOR = 0.2f;
    private final static int BLUR_RADIUS = 5;
    private final static int BLUR_BK_ALPHA = 150;

    // this magic code is defined in surface flinger for our blur screen shot.
    private final static int MAGIC_SCREEN_SHOT = -9999;

    private Context mContext = null;
    private boolean mHasNavigationBar = true;
    private boolean mIsTabletEnv;
    private int mHardwareRotation;

    private int mStatusBarH = 0;
    private int mNavigationBarH = 0;
    private int mStatusBarBlurH = 0;
    private int mNavigationBarBlurH = 0;

    private Bitmap mBmpBlur = null;
    private Bitmap mBmpBlurVis = null;
    private Bitmap mBmpBlurBk = null;

    private Canvas mCanvas = null;
    private Paint mPaint = null;
    private Rect mRcSrc = null;
    private Rect mRcDst = null;
    private Matrix mMatrix = null;

    private int mBkColor = 0x00000000;
    private int mAlpha = 255;

    private Display mDisplay = null;
    private DisplayMetrics mDisplayMetrics = null;

    public BlurBkMaker(Context context) {
        mContext = context;

        // get screen size info include system ui elements, we take the screen shot
        // as blur background should exclude the system ui elements.
        IWindowManager wms = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try { 
            mHasNavigationBar = wms.hasNavigationBar(); 
        } catch (RemoteException e) {}

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);

        mIsTabletEnv = (SystemProperties.getInt("ro.sf.hwrotation", 0) % 180 ) == 90;
        mHardwareRotation = SystemProperties.getInt("ro.sf.hwrotation", 0) / 90;

        mCanvas = new Canvas();
        mPaint = new Paint();
        mRcSrc = new Rect();
        mRcDst = new Rect();
        mMatrix = new Matrix();

        // out blur effect must turn on the AA and AF
        mPaint.setAntiAlias(true); 
        mPaint.setFilterBitmap(true); 
        //mPaint.setAlpha(BLUR_BK_ALPHA);

        loadConfig();
    }

    // this is for support landscape and portrait.
    private void loadConfig() {
        Resources res = mContext.getResources();
        mStatusBarH = res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
        if (mHasNavigationBar) {
            mNavigationBarH = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
        } else { 
            mNavigationBarH = 0;
        }

        // now the don't think about landscape and portrait,
        // because the system ui landscpae and portrait has same size.
        mStatusBarBlurH = (int)(mStatusBarH * BLUR_SIZE_FACTOR) + 1;
        if (0 != mNavigationBarBlurH) {
            mNavigationBarBlurH = (int)(mNavigationBarH * BLUR_SIZE_FACTOR) + 1;
        } else {
            mNavigationBarBlurH = 0;
        }
    }

    public Bitmap getBlurBk() {
        return mBmpBlurBk;
    }

    public int getStatusBarH() {
        return mStatusBarH;
    }

    public int getNavigationBarH() {
        return mNavigationBarH;
    }

    public int getStatusBarBlurH() {
        return mStatusBarBlurH;
    }

    public int getNavigationBarBlurH() {
        return mNavigationBarBlurH;
    }

    public int getScreenW() {
        mDisplay.getRealMetrics(mDisplayMetrics);
        return mDisplayMetrics.widthPixels;
    }

    public int getScreenH() {
        mDisplay.getRealMetrics(mDisplayMetrics);
        return mDisplayMetrics.heightPixels;
    }

    public Paint getDrawPaint() {
        return mPaint;
    }

    public int getDrawColor() {
        return mBkColor;
    }

    public int getDrawAlpha() {
        return mAlpha;
    }

    public void setDrawFraction(float fraction) {
        float f = getInterpolation(fraction);
        mAlpha = (int)(255f * f);
        //Slog.d(TAG, "setDrawFraction: f=" + fraction + ", r=" + f + ", a=" + mAlpha);
        int bk = (int)((float)BLUR_BK_ALPHA * f);
        if (bk > 0xff) bk = 0xff;
        mBkColor = (bk << 24);
        mPaint.setAlpha(mAlpha);
    }

    // the custom interpolation is 2 line combat model
    private float getInterpolation(float input) {
        float result;
        if (input <= 0.15f) {
            result = (float)(3.33f * input);
        } else if (input >= 1.0f){
            result = 1.0f;
        } else {
            result = (float)(0.59f * input + 0.41f);
        }
        return result;
    }

    /**
     * Get a scree shot for blur bacground.
     * @param excludeSystemUIRect True: take screen shot will exclude system ui rect(status bar, navigation bar).
     * @param excludeSystemUILayer True: let surface flinger exclude system ui layer(status bar) when it capture screen
     *
     * @return True: screen shot successj
     */
    public boolean screenshot(boolean excludeSystemUIRect, boolean excludeSystemUILayer) {
        long cost = System.currentTimeMillis(); 

        mDisplay.getRealMetrics(mDisplayMetrics);

        int swap = 0;
        int rot = mDisplay.getRotation();
        int convertRot = convertScreenRotation(rot);

        int screenOrgW = mDisplayMetrics.widthPixels;
        int screenOrgH = mDisplayMetrics.heightPixels;
        int screenW = screenOrgW;
        int screenH = screenOrgH;

        // the samll blur size has a very fast speed, if we let the 
        // blur raidus large, the blur effect is good, is a very clever idea.
        // thank to the XiaoMi engineer. \(@_@)/
        int blurW = (int) (screenW * BLUR_SIZE_FACTOR);
        int blurH = (int) (screenH * BLUR_SIZE_FACTOR);

        int blurVisW = blurW;
        int blurVisH = blurH;
        mRcSrc.set(0, 0, blurVisW, blurVisH);
        mRcDst.set(0, 0, blurVisW, blurVisH);

        // TODO: now the don't think about landscape and portrait,
        // because the system ui landscpae and portrait has same size.
        if (excludeSystemUIRect) {
            blurVisW = blurW;
            blurVisH = blurH - mStatusBarBlurH - mNavigationBarBlurH;
            mRcSrc.set(0, mStatusBarBlurH, blurVisW, mStatusBarBlurH + blurVisH);
            mRcDst.set(0, 0, blurVisW, blurVisH);
        }

        mMatrix.reset(); 
        switch (convertRot) {
        case Surface.ROTATION_90:
            swap = screenW;
            screenW = screenH;
            screenH = swap;

            mMatrix.setRotate(90f, 0f, 0f);
            mMatrix.postTranslate(screenH, 0f);
            mMatrix.postScale(BLUR_SIZE_FACTOR, BLUR_SIZE_FACTOR);
            break;

        case Surface.ROTATION_180:
            mMatrix.setRotate(180f, 0f, 0f);
            mMatrix.postTranslate(screenW, screenH);
            mMatrix.postScale(BLUR_SIZE_FACTOR, BLUR_SIZE_FACTOR);
            break;

        case Surface.ROTATION_270:
            swap = screenW;
            screenW = screenH;
            screenH = swap;

            mMatrix.setRotate(270f, 0f, 0f);
            mMatrix.postTranslate(0f, screenW);
            mMatrix.postScale(BLUR_SIZE_FACTOR, BLUR_SIZE_FACTOR);
            break;

        case Surface.ROTATION_0:
        defualt:
            mMatrix.postScale(BLUR_SIZE_FACTOR, BLUR_SIZE_FACTOR);
            break;
        }

        Slog.d(TAG, String.format("screenshot: rot=%d, hwRot=%d, convertRot=%d, screen:(%dx%d)-->(%dx%d), blur=(%dx%d) blurVis=(%dx%d)", 
                    rot, mHardwareRotation, convertRot,
                    screenOrgW, screenOrgH, screenW, screenH, blurW, blurH, blurVisW, blurVisH 
                    ));
        Slog.d(TAG, "screenshot: rcSrc=" + mRcSrc + ", rcDst=" + mRcDst 
                + ", excludeSystemUIRect=" + excludeSystemUIRect + ", excludeSystemUILayer=" + excludeSystemUILayer);
        excludeSystemUILayer = false;
        // get screen shot from framebuffer
        Bitmap bmpScreen = null;
        if (excludeSystemUILayer) {
//            bmpScreen = Surface.screenshot(screenW, screenH, MAGIC_SCREEN_SHOT, MAGIC_SCREEN_SHOT);
            bmpScreen = SurfaceControl.screenshot(new Rect(),screenW,screenH,MAGIC_SCREEN_SHOT,MAGIC_SCREEN_SHOT,false,Surface.ROTATION_0);
        } else {
//            bmpScreen = Surface.screenshot(screenW, screenH);
        	bmpScreen = SurfaceControl.screenshot(screenW, screenH);
        }
        if (null == bmpScreen) {
            Slog.d(TAG, "taskScreenshot: surface screenshot failed !!");
            Slog.d(TAG, "screen shot take time: " + (System.currentTimeMillis() - cost) + "ms");
            return false;
        }

        // just for debug
        //long debugTime = System.currentTimeMillis(); 
        //DumpUtils.dumpImageToFile(bmpScreen, "/mnt/sdcard/tmp/" + debugTime + "-org.png", Bitmap.CompressFormat.PNG);

        // optimizations
        bmpScreen.setHasAlpha(false);
        //bmpScreen.prepareToDraw();

        mBmpBlur = BitmapUtils.reCreateBitmap(mBmpBlur, blurW, blurH, bmpScreen.getConfig());
        mBmpBlurVis = BitmapUtils.reCreateBitmap(mBmpBlurVis, blurVisW, blurVisH, bmpScreen.getConfig());

        if (!BitmapUtils.checkBitmapValid(mBmpBlur) || !BitmapUtils.checkBitmapValid(mBmpBlurVis)) {
            Slog.d(TAG, "taskScreenshot: no enough memory to create bitmap !!");
            bmpScreen.recycle();
            return false;
        }

        // scale and rotation the screen bitmap to the blur size.
        mBmpBlur.eraseColor(0x00000000);
        mCanvas.setBitmap(mBmpBlur);
        mCanvas.drawBitmap(bmpScreen, mMatrix, null);
        mCanvas.setBitmap(null);

        // just for debug
        //DumpUtils.dumpImageToFile(mBmpBlur, "/mnt/sdcard/tmp/" + debugTime + "-small.png", Bitmap.CompressFormat.PNG);

        // exclude the system ui elements.
        mCanvas.setBitmap(mBmpBlurVis);
        mCanvas.drawBitmap(mBmpBlur, mRcSrc, mRcDst, null);
        mCanvas.setBitmap(null);

        // just for debug
        //DumpUtils.dumpImageToFile(mBmpBlurVis, "/mnt/sdcard/tmp/" + debugTime + "-smallVis.png", Bitmap.CompressFormat.PNG);

        bmpScreen.recycle();
        Slog.d(TAG, "screen shot take time: " + (System.currentTimeMillis() - cost) + "ms");
        return true;
    }

    public void blurScreenshot() {
        long cost = System.currentTimeMillis(); 
        //mBmpBlurBk = BitmapUtils.reCreateBitmap(mBmpBlurBk, 
        //        mBmpBlurVis.getWidth(), mBmpBlurVis.getHeight(), 
        //        mBmpBlurVis.getConfig()); 
        BitmapUtils.freeBitmap(mBmpBlurBk);
        //mBmpBlurBk = mBmpBlurVis.copy(mBmpBlurVis.getConfig(), true); 
        //Surface.fastBlur(mBmpBlurVis, mBmpBlurBk, 25);
        //long debugTime = System.currentTimeMillis(); 
        //DumpUtils.dumpImageToFile(mBmpBlurVis, "/mnt/sdcard/tmp/" + debugTime + "-org.png", Bitmap.CompressFormat.PNG);
        mBmpBlurBk = stackBlur(mBmpBlurVis, BLUR_RADIUS);
        //DumpUtils.dumpImageToFile(mBmpBlurBk, "/mnt/sdcard/tmp/" + debugTime + "-blur.png", Bitmap.CompressFormat.PNG);
        Slog.d(TAG, "blur bitmap take time: " + (System.currentTimeMillis() - cost) + "ms");
        //mBmpBlurBk = mBmpBlurVis;
    }

    private int convertScreenRotation(int rot) {
        // first we rotate the hardware(display driver) to origin, 
        // and then rotate user degree.
        // and remember the hardware rotate direction is ClockWise and user is CounterClockWise. >_<
        rot = (4 - mHardwareRotation) + (4 - rot);
        rot %= 4;
        return rot;
    }

    // Stack Blur v1.0 from
    // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
    //
    // Java Author: Mario Klingemann <mario at quasimondo.com>
    // http://incubator.quasimondo.com
    // created Feburary 29, 2004
    // Android port : Yahel Bouaziz <yahel at kayenko.com>
    // http://www.kayenko.com
    // ported april 5th, 2012

    // This is a compromise between Gaussian Blur and Box blur
    // It creates much better looking blurs than Box Blur, but is
    // 7x faster than my Gaussian Blur implementation.
    //
    // I called it Stack Blur because this describes best how this
    // filter works internally: it creates a kind of moving stack
    // of colors whilst scanning through the image. Thereby it
    // just has to add one new block of color to the right side
    // of the stack and remove the leftmost color. The remaining
    // colors on the topmost layer of the stack are either added on
    // or reduced by one, depending on if they are on the right or
    // on the left side of the stack.
    //
    // If you are using this algorithm in your code please add
    // the following line:
    //
    // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
    private static Bitmap stackBlur(Bitmap inBmp, int radius) {
        if (radius < 1) {
            return (null);
        }

        Bitmap bitmap = inBmp.copy(inBmp.getConfig(), true);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }

}

package dev.r1native.cameracontrols;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

public final class CameraControlsModule extends XposedModule {
    private static final String TARGET = "dev.r1native.shell";
    private static final String TAG = "R1CameraControls";
    private final AtomicBoolean installed = new AtomicBoolean(false);
    private final AtomicBoolean capturePending = new AtomicBoolean(false);
    private volatile int deviceRotation = 0;
    private OrientationEventListener orientationListener;

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (!TARGET.equals(param.getPackageName()) || !param.isFirstPackage() || !installed.compareAndSet(false, true)) {
            return;
        }

        try {
            ClassLoader loader = param.getDefaultClassLoader();
            Class<?> activityClass = Class.forName("dev.r1native.shell.MainActivity", false, loader);
            Class<?> controllerClass = Class.forName("dev.r1native.shell.ShellController", false, loader);
            Class<?> eyeClass = Class.forName("dev.r1native.shell.EyeCamera", false, loader);

            Method onCreate = activityClass.getDeclaredMethod("onCreate", Bundle.class);
            onCreate.setAccessible(true);
            hook(onCreate).intercept(chain -> {
                Object result = chain.proceed();
                Activity activity = (Activity) chain.getThisObject();
                startOrientationTracking(activity);
                installEyeUi(activity);
                return result;
            });

            Method upright = eyeClass.getDeclaredMethod("upright", byte[].class);
            upright.setAccessible(true);
            hook(upright).intercept(chain -> orientJpeg(chain.getThisObject(), (byte[]) chain.getArg(0)));

            Method applyTransform = eyeClass.getDeclaredMethod("applyTransform");
            applyTransform.setAccessible(true);
            hook(applyTransform).intercept(chain -> {
                Object result = chain.proceed();
                correctPreview(chain.getThisObject());
                return result;
            });

            Method request = controllerClass.getDeclaredMethod("request", String.class, String.class, String.class);
            request.setAccessible(true);
            hook(request).intercept(chain -> {
                String action = (String) chain.getArg(1);
                Object controller = chain.getThisObject();
                Activity activity = (Activity) getField(controller, "activity");
                if ("capturePhoto".equals(action)) {
                    activity.runOnUiThread(() -> captureAndSave(activity));
                    return null;
                }
                if ("volumeStep".equals(action)) {
                    String payload = (String) chain.getArg(2);
                    int direction = new JSONObject(payload).optInt("direction", 0);
                    if (direction != 0) adjustMediaVolume(activity, direction);
                    return null;
                }
                return chain.proceed();
            });

            Method emit = activityClass.getDeclaredMethod("emit", String.class, JSONObject.class);
            emit.setAccessible(true);
            hook(emit).intercept(chain -> {
                String event = (String) chain.getArg(0);
                Activity activity = (Activity) chain.getThisObject();
                if ("sideClick".equals(event) && isEyeActive(activity)) {
                    activity.runOnUiThread(() -> captureAndSave(activity));
                    return null;
                }
                return chain.proceed();
            });

            log(Log.INFO, TAG, "Eye and hardware controls installed");
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Could not install camera hooks", error);
        }
    }

    private void startOrientationTracking(Activity activity) {
        if (orientationListener != null) return;
        orientationListener = new OrientationEventListener(activity) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;
                int next = ((orientation + 45) / 90 * 90) % 360;
                if (next == deviceRotation) return;
                deviceRotation = next;
                log(Log.INFO, TAG, "Device camera rotation: " + next);
                activity.runOnUiThread(() -> {
                    try {
                        Object eye = getField(activity, "eye");
                        if (eye != null && (boolean) getField(eye, "active")) {
                            Method transform = eye.getClass().getDeclaredMethod("applyTransform");
                            transform.setAccessible(true);
                            transform.invoke(eye);
                        }
                    } catch (Throwable error) {
                        log(Log.WARN, TAG, "Could not refresh camera preview", error);
                    }
                });
            }
        };
        if (orientationListener.canDetectOrientation()) orientationListener.enable();
    }

    private byte[] orientJpeg(Object eye, byte[] raw) {
        Bitmap src = null;
        Bitmap rotated = null;
        try {
            int sensor = (int) getField(eye, "orientation");
            int angle = (int) getField(eye, "angle");
            boolean front = angle >= 135;
            int degrees = front
                    ? (sensor + deviceRotation) % 360
                    : (sensor - deviceRotation + 360) % 360;
            if (degrees == 0) return raw;

            src = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (src == null) return raw;
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            rotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            rotated.compress(Bitmap.CompressFormat.JPEG, 92, out);
            log(Log.INFO, TAG, "Oriented JPEG " + degrees + " degrees for device rotation " + deviceRotation);
            return out.toByteArray();
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Could not orient JPEG", error);
            return raw;
        } finally {
            if (rotated != null && rotated != src) rotated.recycle();
            if (src != null) src.recycle();
        }
    }

    private void correctPreview(Object eye) {
        try {
            TextureView texture = (TextureView) getField(eye, "texture");
            if (texture == null || texture.getWidth() == 0 || texture.getHeight() == 0) return;
            int angle = (int) getField(eye, "angle");
            boolean front = angle >= 135;
            float cx = texture.getWidth() / 2f;
            float cy = texture.getHeight() / 2f;
            Matrix matrix = new Matrix();
            texture.getTransform(matrix);
            if (front) {
                // The launcher applies a vertical flip here, which turns the R1 motor camera upside down.
                matrix.postScale(1f, -1f, cx, cy);
                matrix.postRotate(deviceRotation, cx, cy);
            } else {
                matrix.postRotate(-deviceRotation, cx, cy);
            }
            texture.setTransform(matrix);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Could not correct camera preview", error);
        }
    }

    private void installEyeUi(Activity activity) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                WebView web = (WebView) getField(activity, "web");
                if (web == null) return;
                String script = "(()=>{" +
                        "if(document.getElementById('eye-capture'))return;" +
                        "const bar=document.querySelector('.eye-bar');if(!bar)return;" +
                        "const b=document.createElement('button');b.id='eye-capture';b.textContent='CAPTURE ◉';" +
                        "b.onclick=(e)=>{e.stopPropagation();b.textContent='SAVING…';" +
                        "try{R1Bridge.request('photo-'+Date.now(),'capturePhoto','{}')}catch(_){b.textContent='RETRY'}" +
                        "setTimeout(()=>b.textContent='CAPTURE ◉',1200)};" +
                        "const close=document.getElementById('eye-close');bar.insertBefore(b,close);" +
                        "const h=document.createElement('div');h.id='eye-photo-hint';" +
                        "h.textContent='POWER: PHOTO  •  HOLD: ASK ABOUT VIEW';" +
                        "h.style.cssText='font-size:9px;opacity:.68;text-align:center;letter-spacing:.05em;margin-top:4px';" +
                        "bar.parentNode.insertBefore(h,bar.nextSibling);" +
                        "const hw=document.querySelector('.hardware-card');" +
                        "if(hw&&!hw.dataset.volumeSwipe){hw.dataset.volumeSwipe='1';" +
                        "const hint=document.createElement('div');hint.id='ctrl-volume-swipe';" +
                        "hint.textContent='VOLUME  ◀  SWIPE  ▶';" +
                        "hint.style.cssText='font:9px monospace;color:#ff4d00;text-align:center;height:13px;line-height:13px;letter-spacing:.04em';" +
                        "const list=document.getElementById('hw-list');list.parentNode.insertBefore(hint,list);" +
                        "hw.style.touchAction='pan-y';" +
                        "let sx=0,sy=0,lx=0,tracking=false,horizontal=false,swallow=false,seq=0;" +
                        "hw.addEventListener('pointerdown',e=>{const s=window.R1Shell?.getState?.();" +
                        "if(!s||s.page!==3||s.sheet)return;tracking=true;horizontal=false;sx=lx=e.clientX;sy=e.clientY;" +
                        "try{hw.setPointerCapture(e.pointerId)}catch(_){}});" +
                        "hw.addEventListener('pointermove',e=>{if(!tracking)return;const dx=e.clientX-sx,dy=e.clientY-sy;" +
                        "if(!horizontal){if(Math.abs(dx)<6)return;if(Math.abs(dx)<=Math.abs(dy)*1.15)return;horizontal=true;}" +
                        "e.preventDefault();while(Math.abs(e.clientX-lx)>=10){const d=e.clientX>lx?1:-1;" +
                        "window.R1Hw?.adjust?.('volume',d);try{R1Bridge.request('volume-'+(++seq),'volumeStep',JSON.stringify({direction:d}))}catch(_){}" +
                        "lx+=d*10;}} ,{passive:false});" +
                        "const end=e=>{if(horizontal){swallow=true;setTimeout(()=>swallow=false,80)}tracking=false;horizontal=false;" +
                        "try{hw.releasePointerCapture(e.pointerId)}catch(_){}};" +
                        "hw.addEventListener('pointerup',end);hw.addEventListener('pointercancel',end);" +
                        "hw.addEventListener('click',e=>{if(swallow){e.preventDefault();e.stopImmediatePropagation();}},true);}" +
                        "})();";
                web.evaluateJavascript(script, null);
            } catch (Throwable error) {
                log(Log.ERROR, TAG, "Could not add Eye controls", error);
            }
        }, 1400);
    }

    private void adjustMediaVolume(Activity activity, int direction) {
        try {
            AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (audio == null) return;
            int adjustment = direction > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjustment, 0);
            log(Log.INFO, TAG, direction > 0 ? "Hardware swipe volume up" : "Hardware swipe volume down");
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Could not adjust media volume", error);
        }
    }

    private boolean isEyeActive(Activity activity) {
        try {
            Object eye = getField(activity, "eye");
            return eye != null && (boolean) getField(eye, "active");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void captureAndSave(Activity activity) {
        if (!isEyeActive(activity)) {
            toast(activity, "Open Eye before taking a photo");
            return;
        }
        if (!capturePending.compareAndSet(false, true)) {
            toast(activity, "Photo capture already in progress");
            return;
        }

        try {
            Object eye = getField(activity, "eye");
            Class<?> callbackType = Class.forName("dev.r1native.shell.EyeCamera$FrameCallback", false, activity.getClassLoader());
            Object callback = Proxy.newProxyInstance(activity.getClassLoader(), new Class<?>[]{callbackType}, (proxy, method, args) -> {
                if ("result".equals(method.getName())) {
                    capturePending.set(false);
                    byte[] jpeg = args != null && args.length > 0 ? (byte[]) args[0] : null;
                    String error = args != null && args.length > 1 ? (String) args[1] : null;
                    if (jpeg == null) {
                        toast(activity, error == null ? "Photo capture failed" : error);
                    } else {
                        saveToGallery(activity, jpeg);
                    }
                    return null;
                }
                if ("toString".equals(method.getName())) return "RabbitR1PhotoCallback";
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                if ("equals".equals(method.getName())) return proxy == args[0];
                return null;
            });
            Method capture = eye.getClass().getMethod("capture", callbackType);
            capture.invoke(eye, callback);
        } catch (Throwable error) {
            capturePending.set(false);
            log(Log.ERROR, TAG, "Photo capture failed", error);
            toast(activity, "Photo capture failed");
        }
    }

    private void saveToGallery(Activity activity, byte[] jpeg) {
        Uri item = null;
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "R1_" + timestamp + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/RabbitR1");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            ContentResolver resolver = activity.getContentResolver();
            item = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (item == null) throw new IllegalStateException("MediaStore insert failed");
            try (OutputStream out = resolver.openOutputStream(item, "w")) {
                if (out == null) throw new IllegalStateException("MediaStore output failed");
                out.write(jpeg);
            }
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(item, values, null, null);
            vibrate(activity);
            toast(activity, "Photo saved to DCIM/RabbitR1");
            log(Log.INFO, TAG, "Saved Eye photo: " + item + " (" + jpeg.length + " bytes)");
        } catch (Throwable error) {
            if (item != null) activity.getContentResolver().delete(item, null, null);
            log(Log.ERROR, TAG, "Could not save photo", error);
            toast(activity, "Could not save photo");
        }
    }

    private void vibrate(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(VibrationEffect.createOneShot(45, 110));
        } catch (Throwable ignored) {
        }
    }

    private void toast(Activity activity, String text) {
        activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
    }

    private Object getField(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}

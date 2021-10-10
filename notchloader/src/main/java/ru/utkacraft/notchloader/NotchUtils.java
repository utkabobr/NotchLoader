package ru.utkacraft.notchloader;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;

import androidx.core.graphics.PathParser;

public class NotchUtils {
    private static final String TAG = "NotchUtils";
    private static final String BOTTOM_MARKER = "@bottom";
    private static final String DP_MARKER = "@dp";
    private static final String RIGHT_MARKER = "@right";
    private static final String LEFT_MARKER = "@left";

    public static LoaderMeta createLoaderMeta(Context ctx) {
        LoaderMeta meta = new LoaderMeta();
        meta.loaderType = LoaderType.DEFAULT;
        int r = ctx.getResources().getIdentifier("config_mainBuiltInDisplayCutout", "string", "android");
        if (r != 0) {
            String spec = ctx.getString(r);
            if (spec.isEmpty())
                return meta;
            spec = spec.trim();

            Resources res = ctx.getResources();
            DisplayMetrics m = res.getDisplayMetrics();
            int displayWidth = m.widthPixels;
            float density = m.density;

            int gravity;

            float offsetX;
            if (spec.endsWith(RIGHT_MARKER)) {
                offsetX = displayWidth;
                spec = spec.substring(0, spec.length() - RIGHT_MARKER.length()).trim();
                gravity = Gravity.END;
            } else if (spec.endsWith(LEFT_MARKER)) {
                offsetX = 0;
                spec = spec.substring(0, spec.length() - LEFT_MARKER.length()).trim();
                gravity = Gravity.START;
            } else {
                offsetX = displayWidth / 2f;
                gravity = Gravity.CENTER;
            }
            boolean inDp = spec.endsWith(DP_MARKER);
            if (inDp) {
                spec = spec.substring(0, spec.length() - DP_MARKER.length());
            }

            if (spec.contains(BOTTOM_MARKER)) {
                String[] splits = spec.split(BOTTOM_MARKER, 2);
                spec = splits[0].trim();
            }

            Path p;
            try {
                PathParser.PathDataNode[] n = PathParser.createNodesFromPathData(spec);

                p = new Path();
                PathParser.PathDataNode.nodesToPath(n, p);
            } catch (Throwable e) {
                Log.wtf(TAG, "Could not inflate cutout: ", e);
                return meta;
            }

            Matrix matrix = new Matrix();
            if (inDp) {
                matrix.postScale(density, density);
            }
            matrix.postTranslate(offsetX, 0);
            p.transform(matrix);

            meta.path = p;

            RectF b = new RectF();
            p.computeBounds(b, false);

            if (gravity == Gravity.CENTER) {
                if (spec.contains("C") || spec.contains("S") || spec.contains("Q")) {
                    meta.loaderType = LoaderType.OUTLINE_NOTCH;
                    return meta;
                } else {
                    meta.loaderType = LoaderType.MINI_LOADER;
                    return meta;
                }
            } else {
                meta.loaderType = LoaderType.DEFAULT;
                if (gravity == Gravity.START) {
                    meta.marginStart = meta.marginEnd = (int) (b.left + b.width());
                } else {
                    meta.marginEnd = meta.marginStart = (int) (m.widthPixels - b.left);
                }
                return meta;
            }
        }
        return meta;
    }

    public enum LoaderType {
        OUTLINE_NOTCH,
        MINI_LOADER,
        DEFAULT
    }

    public final static class LoaderMeta {
        public LoaderType loaderType;
        public int marginStart, marginEnd;
        public Path path;
    }
}

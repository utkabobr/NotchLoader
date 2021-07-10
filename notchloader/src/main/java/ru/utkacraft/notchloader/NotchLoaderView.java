package ru.utkacraft.notchloader;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

public class NotchLoaderView extends FrameLayout {
    private final static SpringSystem springSystem = SpringSystem.create();
    private Spring mSpring;

    // Default and mini views
    private ProgressBar loaderView;
    private TextView titleView;
    private int mTextColor;

    // Notch outline fields
    private boolean animateOutline;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path mPath;
    private RectF notchBounds = new RectF();
    private int mOutlineColor;
    private int strokeWidth;
    private boolean inverseAnim;
    private Spring mAppearSpring;

    // Shared fields
    private NotchUtils.LoaderMeta loaderMeta;
    private boolean hideSystemUi;
    private int statusBarHeight;
    private boolean pendingEnd;
    private boolean appearPending;

    public NotchLoaderView(Context context) {
        super(context);
    }

    public NotchLoaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NotchLoaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NotchLoaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        loaderMeta = NotchUtils.createLoaderMeta(getContext());
        hideSystemUi = loaderMeta.loaderType == NotchUtils.LoaderType.DEFAULT || loaderMeta.loaderType == NotchUtils.LoaderType.MINI_LOADER;
        int icon = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        int color = mTextColor;

        titleView = new TextView(getContext());
        titleView.setText(R.string.notch_loading);
        titleView.setTextColor(color);

        loaderView = new ProgressBar(getContext());
        loaderView.setSelected(true);
        loaderView.setIndeterminateTintList(ColorStateList.valueOf(color));

        loaderView.setLayoutParams(new LayoutParams(icon, icon));

        setTextColor(Color.RED);
        setOutlineColor(Color.RED);
        switch (loaderMeta.loaderType) {
            case MINI_LOADER:
            case DEFAULT: {
                titleView.setTextSize(12);

                LinearLayout ll = new LinearLayout(getContext());
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.addView(loaderView);
                LayoutParams tParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tParams.setMarginStart(pad);
                ll.addView(titleView, tParams);
                LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = (loaderMeta.loaderType == NotchUtils.LoaderType.DEFAULT ? Gravity.CENTER_HORIZONTAL : Gravity.START) | Gravity.CENTER;
                params.setMarginStart(loaderMeta.loaderType == NotchUtils.LoaderType.DEFAULT ? loaderMeta.marginStart : pad);
                params.setMarginEnd(loaderMeta.marginEnd);
                addView(ll, params);
                if (loaderMeta.loaderType == NotchUtils.LoaderType.MINI_LOADER) {
                    ll.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(statusBarHeight, MeasureSpec.EXACTLY));
                    int w = ll.getMeasuredWidth();
                    RectF b = new RectF();
                    loaderMeta.path.computeBounds(b, false);
                    if (params.getMarginStart() + w + pad >= b.left) {
                        titleView.setVisibility(View.GONE);
                    }
                }

                break;
            }
            case OUTLINE_NOTCH:
                setWillNotDraw(false);
                mPath = new Path(loaderMeta.path);
                mPath.computeBounds(notchBounds, false);
                animateOutline = true;

                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeWidth(strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
                mPath.moveTo(0, 0);
                mPath.lineTo(getResources().getDisplayMetrics().widthPixels, 0);

                invalidate();
                break;
        }
        invalidateShader(0);
        invalidateSystemUi();
    }

    /**
     * Sets new text to the title
     * @param text New text to be set
     */
    public void setText(CharSequence text) {
        if (titleView != null)
            titleView.setText(text);
    }

    /**
     * Attempts to appear on the screen
     */
    public void tryAppear() {
        if (appearPending || getVisibility() == VISIBLE)
            return;

        if (loaderMeta.loaderType == NotchUtils.LoaderType.OUTLINE_NOTCH) {
            mSpring = null;
            inverseAnim = false;
            invalidateShader(0);
            setAlpha(0);
            invalidate();
        } else {
            setTranslationY(-getMeasuredHeight());
        }
        setVisibility(VISIBLE);

        appearPending = true;
        if (mAppearSpring != null) {
            mAppearSpring.destroy();
            mAppearSpring = null;
        }
        mAppearSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(95, 13))
                .setOvershootClampingEnabled(true)
                .setCurrentValue(0)
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        float val = (float) spring.getCurrentValue();
                        if (val >= 1) {
                            setAlpha(1);
                            setTranslationY(0);
                            appearPending = false;

                            if (loaderMeta.loaderType == NotchUtils.LoaderType.OUTLINE_NOTCH) {
                                if (mSpring == null || springSystem.getSpringById(mSpring.getId()) == null) {
                                    mSpring = null;
                                    createNotchSpring();
                                }
                                inverseAnim = true;
                                mSpring.setCurrentValue(0);
                                mSpring.setEndValue(1);
                            }
                        } else {
                            if (loaderMeta.loaderType == NotchUtils.LoaderType.OUTLINE_NOTCH) {
                                setAlpha(val);
                            } else {
                                setTranslationY(-getMeasuredHeight() * (1f - val));
                            }
                        }
                    }

                    @Override
                    public void onSpringAtRest(Spring spring) {
                        spring.destroy();
                        mAppearSpring = null;
                    }
                })
                .setEndValue(1);
    }

    /**
     * Animates end of the loading
     */
    public void tryEnd() {
        if (pendingEnd || getVisibility() == GONE)
            return;

        pendingEnd = true;
        if (loaderMeta.loaderType != NotchUtils.LoaderType.OUTLINE_NOTCH) {
            springSystem.createSpring()
                    .setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(95, 13))
                    .setOvershootClampingEnabled(true)
                    .setCurrentValue(0)
                    .addListener(new SimpleSpringListener() {
                        @Override
                        public void onSpringUpdate(Spring spring) {
                            float val = (float) spring.getCurrentValue();
                            if (val >= 1) {
                                setVisibility(GONE);
                                setTranslationY(1);
                                spring.destroy();
                                pendingEnd = false;
                            } else {
                                setTranslationY(-getMeasuredHeight() * val);
                            }
                        }
                    })
                    .setEndValue(1);
        }
    }

    /**
     * Animates disappear of the view
     */
    private void animateAlphaDisappear() {
        springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(95, 13))
                .setOvershootClampingEnabled(true)
                .setCurrentValue(1)
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        float val = (float) spring.getCurrentValue();
                        if (val <= 0) {
                            setVisibility(View.GONE);
                            setAlpha(1);
                            spring.destroy();
                        } else {
                            setAlpha(val);
                        }
                    }
                })
                .setEndValue(0);
    }

    /**
     * Sets new text color
     * @param mTextColor New text color
     */
    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
        if (titleView != null)
            titleView.setTextColor(mTextColor);
        if (loaderView != null)
            loaderView.setIndeterminateTintList(ColorStateList.valueOf(mTextColor));
    }

    /**
     * Sets new outline color
     * @param mOutlineColor New color
     */
    public void setOutlineColor(int mOutlineColor) {
        this.mOutlineColor = mOutlineColor;
        invalidateShader(mSpring != null ? (float) mSpring.getCurrentValue() : 0);
        invalidate();
    }

    /**
     * Invalidates gradient shader for the path
     */
    private void invalidateShader(float val) {
        int[] colors = inverseAnim ? new int[] {0, 0, mOutlineColor, mOutlineColor} : new int[]{mOutlineColor, mOutlineColor, 0, 0};
        float[] positions = new float[colors.length];
        positions[0] = 0;
        positions[1] = positions[2] = val;
        positions[3] = 1;

        paint.setShader(new LinearGradient(0, 0, getWidth(), 0, colors, positions, Shader.TileMode.MIRROR));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            statusBarHeight = getRootWindowInsets().getSystemWindowInsetTop();
        } else statusBarHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());

        if (animateOutline) {
            createNotchSpring();
        }

        requestLayout();
    }

    /**
     * Creates spring for the notch animation
     */
    private void createNotchSpring() {
        if (mSpring != null) {
            mSpring.destroy();
            mSpring = null;
        }
        mSpring = springSystem.createSpring()
                .setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(10, 10))
                .setOvershootClampingEnabled(true)
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        if (getVisibility() != VISIBLE)
                            return;

                        if (animateOutline) {
                            float val = (float) spring.getCurrentValue();
                            if (val >= 1) {
                                if (pendingEnd) {
                                    spring.destroy();
                                    animateAlphaDisappear();
                                    pendingEnd = false;
                                    return;
                                }
                                inverseAnim = !inverseAnim;
                                spring.setCurrentValue(0, true);
                                spring.setEndValue(1);
                            } else {
                                invalidateShader(val);
                                invalidate();
                            }
                        }
                    }
                }).setEndValue(1);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mSpring != null)
            mSpring.destroy();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(statusBarHeight + strokeWidth, MeasureSpec.EXACTLY));
    }

    /**
     * Invalidates system ui flags
     */
    private void invalidateSystemUi() {
        setSystemUiVisibility(getSystemUiVisibility());
    }

    @Override
    public void setSystemUiVisibility(int visibility) {
        if (hideSystemUi) {
            visibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        } else visibility &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;

        super.setSystemUiVisibility(visibility);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (animateOutline) {
            canvas.drawPath(mPath, paint);
        }
    }

    public static NotchLoaderView install(Activity act) {
        NotchLoaderView l = new NotchLoaderView(act);
        l.setTranslationZ(Float.MAX_VALUE);
        l.setVisibility(View.GONE);

        ViewGroup d = (ViewGroup) act.getWindow().getDecorView();
        d.addView(l);

        return l;
    }
}

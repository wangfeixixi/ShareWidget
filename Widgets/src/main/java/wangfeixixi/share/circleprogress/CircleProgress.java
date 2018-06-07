package wangfeixixi.share.circleprogress;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

import wangfeixixi.share.R;


public class CircleProgress extends ProgressBar {
    private static final String TAG = CircleProgress.class.getSimpleName();

    private Paint mPaint;
    private Mode mMode;
    private int mTextColor;
    private int mTextSize;
    private int mTextMargin;
    private int mReachedColor;
    private int mReachedHeight;
    private int mUnReachedColor;
    private int mUnReachedHeight;
    private boolean mIsCapRounded;
    private boolean mIsHiddenText;

    private int mRadius;

    private int mMaxUnReachedEndX;
    private int mMaxStrokeWidth;

    private int mTextHeight;
    private int mTextWidth;

    private RectF mArcRectF;
    private Rect mTextRect = new Rect();

    private String mText;

    public CircleProgress(Context context) {
        this(context, null);
    }

    public CircleProgress(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.progressBarStyle);
    }

    public CircleProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDefaultAttrs(context);
        initCustomAttrs(context, attrs);

        mMaxStrokeWidth = Math.max(mReachedHeight, mUnReachedHeight);
    }

    private void initDefaultAttrs(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mMode = Mode.System;
        mTextColor = Color.parseColor("#70A800");
        mTextSize = CircleProgress.sp2px(context, 10);
        mTextMargin = CircleProgress.dp2px(context, 4);
        mReachedColor = Color.parseColor("#70A800");
        mReachedHeight = CircleProgress.dp2px(context, 2);
        mUnReachedColor = Color.parseColor("#CCCCCC");
        mUnReachedHeight = CircleProgress.dp2px(context, 1);
        mIsCapRounded = false;
        mIsHiddenText = false;

        mRadius = CircleProgress.dp2px(context, 16);
    }

    private void initCustomAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress);
        final int N = typedArray.getIndexCount();
        for (int i = 0; i < N; i++) {
            initAttr(typedArray.getIndex(i), typedArray);
        }
        typedArray.recycle();
    }

    protected void initAttr(int attr, TypedArray typedArray) {
        if (attr == R.styleable.CircleProgress_bga_pb_mode) {
            int ordinal = typedArray.getInt(attr, Mode.System.ordinal());
            mMode = Mode.values()[ordinal];
        } else if (attr == R.styleable.CircleProgress_bga_pb_textColor) {
            mTextColor = typedArray.getColor(attr, mTextColor);
        } else if (attr == R.styleable.CircleProgress_bga_pb_textSize) {
            mTextSize = typedArray.getDimensionPixelOffset(attr, mTextSize);
        } else if (attr == R.styleable.CircleProgress_bga_pb_textMargin) {
            mTextMargin = typedArray.getDimensionPixelOffset(attr, mTextMargin);
        } else if (attr == R.styleable.CircleProgress_bga_pb_reachedColor) {
            mReachedColor = typedArray.getColor(attr, mReachedColor);
        } else if (attr == R.styleable.CircleProgress_bga_pb_reachedHeight) {
            mReachedHeight = typedArray.getDimensionPixelOffset(attr, mReachedHeight);
        } else if (attr == R.styleable.CircleProgress_bga_pb_unReachedColor) {
            mUnReachedColor = typedArray.getColor(attr, mUnReachedColor);
        } else if (attr == R.styleable.CircleProgress_bga_pb_unReachedHeight) {
            mUnReachedHeight = typedArray.getDimensionPixelOffset(attr, mUnReachedHeight);
        } else if (attr == R.styleable.CircleProgress_bga_pb_isCapRounded) {
            mIsCapRounded = typedArray.getBoolean(attr, mIsCapRounded);
            if (mIsCapRounded) {
                mPaint.setStrokeCap(Paint.Cap.ROUND);
            }
        } else if (attr == R.styleable.CircleProgress_bga_pb_isHiddenText) {
            mIsHiddenText = typedArray.getBoolean(attr, mIsHiddenText);
        } else if (attr == R.styleable.CircleProgress_bga_pb_radius) {
            mRadius = typedArray.getDimensionPixelOffset(attr, mRadius);
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMode == Mode.System) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else if (mMode == Mode.Horizontal) {
            calculateTextWidthAndHeight();

            int width = MeasureSpec.getSize(widthMeasureSpec);

            int expectHeight = getPaddingTop() + getPaddingBottom();
            if (mIsHiddenText) {
                expectHeight += Math.max(mReachedHeight, mUnReachedHeight);
            } else {
                expectHeight += Math.max(mTextHeight, Math.max(mReachedHeight, mUnReachedHeight));
            }
            int height = resolveSize(expectHeight, heightMeasureSpec);
            setMeasuredDimension(width, height);

            mMaxUnReachedEndX = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        } else if (mMode == Mode.Circle) {
            int expectSize = mRadius * 2 + mMaxStrokeWidth + getPaddingLeft() + getPaddingRight();
            int width = resolveSize(expectSize, widthMeasureSpec);
            int height = resolveSize(expectSize, heightMeasureSpec);
            expectSize = Math.min(width, height);

            mRadius = (expectSize - getPaddingLeft() - getPaddingRight() - mMaxStrokeWidth) / 2;
            if (mArcRectF == null) {
                mArcRectF = new RectF();
            }
            mArcRectF.set(0, 0, mRadius * 2, mRadius * 2);

            setMeasuredDimension(expectSize, expectSize);
        } else if (mMode == Mode.Comet) {
            // TODO
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else if (mMode == Mode.Wave) {
            // TODO
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (mMode == Mode.System) {
            super.onDraw(canvas);
        } else if (mMode == Mode.Horizontal) {
            onDrawHorizontal(canvas);
        } else if (mMode == Mode.Circle) {
            onDrawCircle(canvas);
        } else if (mMode == Mode.Comet) {
            // TODO
            super.onDraw(canvas);
        } else if (mMode == Mode.Wave) {
            // TODO
            super.onDraw(canvas);
        }
    }

    private void onDrawHorizontal(Canvas canvas) {
        canvas.save();
        canvas.translate(getPaddingLeft(), getMeasuredHeight() / 2);

        float reachedRatio = getProgress() * 1.0f / getMax();
        float reachedEndX = reachedRatio * mMaxUnReachedEndX;

        if (mIsHiddenText) {
            if (reachedEndX > mMaxUnReachedEndX) {
                reachedEndX = mMaxUnReachedEndX;
            }
            if (reachedEndX > 0) {
                mPaint.setColor(mReachedColor);
                mPaint.setStrokeWidth(mReachedHeight);
                mPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(0, 0, reachedEndX, 0, mPaint);
            }

            float unReachedStartX = reachedEndX;
            if (mIsCapRounded) {
                unReachedStartX += (mReachedHeight + mUnReachedHeight) * 1.0f / 2;
            }
            if (unReachedStartX < mMaxUnReachedEndX) {
                mPaint.setColor(mUnReachedColor);
                mPaint.setStrokeWidth(mUnReachedHeight);
                mPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(unReachedStartX, 0, mMaxUnReachedEndX, 0, mPaint);
            }
        } else {
            calculateTextWidthAndHeight();
            int maxReachedEndX = mMaxUnReachedEndX - mTextWidth - mTextMargin;
            if (reachedEndX > maxReachedEndX) {
                reachedEndX = maxReachedEndX;
            }
            if (reachedEndX > 0) {
                mPaint.setColor(mReachedColor);
                mPaint.setStrokeWidth(mReachedHeight);
                mPaint.setStyle(Paint.Style.STROKE);

                canvas.drawLine(0, 0, reachedEndX, 0, mPaint);
            }

            mPaint.setTextAlign(Paint.Align.LEFT);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mTextColor);
            float textStartX = reachedEndX > 0 ? reachedEndX + mTextMargin : reachedEndX;
            canvas.drawText(mText, textStartX, mTextHeight / 2, mPaint);

            float unReachedStartX = textStartX + mTextWidth + mTextMargin;
            if (unReachedStartX < mMaxUnReachedEndX) {
                mPaint.setColor(mUnReachedColor);
                mPaint.setStrokeWidth(mUnReachedHeight);
                mPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(unReachedStartX, 0, mMaxUnReachedEndX, 0, mPaint);
            }
        }

        canvas.restore();
    }

    private void onDrawCircle(Canvas canvas) {
        canvas.save();
        canvas.translate(getPaddingLeft() + mMaxStrokeWidth / 2, getPaddingTop() + mMaxStrokeWidth / 2);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mUnReachedColor);
        mPaint.setStrokeWidth(mUnReachedHeight);
        canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mReachedColor);
        mPaint.setStrokeWidth(mReachedHeight);
        float sweepAngle = getProgress() * 1.0f / getMax() * 360;
        canvas.drawArc(mArcRectF, 0, sweepAngle, false, mPaint);

        if (!mIsHiddenText) {
            calculateTextWidthAndHeight();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mTextColor);
            mPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(mText, mRadius, mRadius + mTextHeight / 2, mPaint);
        }

        canvas.restore();
    }

    private void calculateTextWidthAndHeight() {
        //fix by Michael 修改参数溢出问题。
        //mText = String.format("%d", getProgress() * 100 / getMax()) + "%";
        mText = String.format("%d", (int) (getProgress() * 1.0f / getMax() * 100)) + "%";
        mPaint.setTextSize(mTextSize);
        mPaint.setStyle(Paint.Style.FILL);

        mPaint.getTextBounds(mText, 0, mText.length(), mTextRect);
        mTextWidth = mTextRect.width();
        mTextHeight = mTextRect.height();
    }

    private enum Mode {
        System,
        Horizontal,
        Circle,
        Comet,
        Wave
    }

    public static int dp2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    }

    public static int sp2px(Context context, float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.getResources().getDisplayMetrics());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("aaaaaaaaa", "ACTION_DOWN");
                mHandler.sendEmptyMessage(0);
                return true;
            case MotionEvent.ACTION_MOVE:
                Log.d("aaaaaaaaa", "ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.d("aaaaaaaaa", "ACTION_UP");
                mHandler.sendEmptyMessage(1);
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.d("aaaaaaaaa", "ACTION_CANCEL");
                mHandler.sendEmptyMessage(1);
                break;
        }
        return super.onTouchEvent(event);
    }

    private ProgressHandler mHandler = new ProgressHandler(this);


    private static class ProgressHandler extends Handler {

        private WeakReference<CircleProgress> mainActivityWeakReference;

        private int progress = 0;

        public ProgressHandler(CircleProgress mainActivity) {
            mainActivityWeakReference = new WeakReference<CircleProgress>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0://开始
                    removeCallbacksAndMessages(null);
                    sendEmptyMessage(2);
                    break;
                case 1://暂停
                    removeCallbacksAndMessages(null);
                    sendEmptyMessage(3);
                    break;
                case 2://增加
                    removeCallbacksAndMessages(null);
                    if (progress == 100) {
                        return;
                    }
                    sendEmptyMessageDelayed(2, 50);
                    mainActivityWeakReference.get().setProgress(progress += 2);
                    if (mainActivityWeakReference.get().mOnProccess != null)
                        mainActivityWeakReference.get().mOnProccess.proccess(progress);
                    break;
                case 3://减少
                    removeCallbacksAndMessages(null);
                    if (progress == 0 || progress == 100) {
                        return;
                    }
                    sendEmptyMessageDelayed(3, 50);
                    mainActivityWeakReference.get().setProgress(progress -= 2);
                    if (mainActivityWeakReference.get().mOnProccess != null)
                        mainActivityWeakReference.get().mOnProccess.proccess(progress);
                    break;
                case 4://重置
                    removeCallbacksAndMessages(null);
                    progress = 0;
                    mainActivityWeakReference.get().setProgress(progress);
                    break;
            }
        }
    }

    public void reset() {
        mHandler.sendEmptyMessage(4);
    }

    public void setOnProcessListener(OnProccessListener onProcessListener) {
        mOnProccess = onProcessListener;
    }

    private OnProccessListener mOnProccess;

    public interface OnProccessListener {
        void proccess(int process);
    }
}
package huainan.kidyn.cn.mymediarecord;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.BoolRes;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Vison on 2018/1/29.
 */

public class MyProgressbar extends View {

    private int strokeWidth;

    private int strokeColor;

    private float circleRadius;

    private int circleColor;

    private Paint strokePaint;

    private Paint circlePaint;

    private int centerX;
    private int centerY;

    private float currentProgress;//当前进度值，总进度值为360

    private TextPaint mTextPaint;
    private Paint.FontMetrics fm;
    private String currentText = "60s";
    private float textWidth;//字符宽度
    private float totalRadius;
    private float incrementRadius;

    public MyProgressbar(Context context) {
        this(context,null);
    }

    public MyProgressbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MyProgressbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.MyProgressbar,defStyleAttr,0);
        strokeWidth = array.getInteger(R.styleable.MyProgressbar_stoke_width,5);
        strokeColor = array.getColor(R.styleable.MyProgressbar_stoke_color,getResources().getColor(R.color.colorPrimaryDark));
        circleRadius = array.getDimensionPixelSize(R.styleable.MyProgressbar_circle_radius, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10,getResources().getDisplayMetrics()));
        circleColor = array.getColor(R.styleable.MyProgressbar_circle_bg,getResources().getColor(R.color.white));
        incrementRadius = array.getInteger(R.styleable.MyProgressbar_radius_increment,0);
        array.recycle();

        strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(strokeColor);

        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(circleColor);

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(16);
        mTextPaint.setColor(getResources().getColor(R.color.colorPrimaryDark));
        fm = mTextPaint.getFontMetrics();
        totalRadius = circleRadius + incrementRadius;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpec = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpec = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpec != MeasureSpec.EXACTLY) {
            width = (int) ((strokeWidth + circleRadius + 5) * 2);
        }

        if (heightSpec != MeasureSpec.EXACTLY) {
            height = (int) ((strokeWidth + circleRadius + 5) * 2);
        }
        centerX = width / 2;
        centerY = height / 2;
        setMeasuredDimension(width,height);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float baseLine = getHeight() / 2 - fm.descent + (fm.bottom - fm.top) / 2; // 因为是数字，所以不使用top和bottom
        //画圆弧
        canvas.drawCircle(centerX,centerY,circleRadius,circlePaint);
        //根据进度值画边界
        int top = (int) (centerX - strokeWidth - circleRadius);
        int left = (int) (centerY - strokeWidth - circleRadius);
        int right = (int) (centerX + strokeWidth + circleRadius);
        int bottom = (int) (centerY + strokeWidth + circleRadius);

        RectF rectF = new RectF(left,top,right,bottom);
        textWidth = mTextPaint.measureText(currentText);
        canvas.drawArc(rectF,-90,currentProgress,false,strokePaint);
        canvas.drawText(currentText, centerX - textWidth / 2, baseLine, mTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (MotionEventCompat.getActionMasked(event)){
            case MotionEvent.ACTION_DOWN:
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(circleRadius,totalRadius).setDuration(100);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        circleRadius = (float) valueAnimator.getAnimatedValue();
                        requestLayout();
                        invalidate();
                    }
                });
                valueAnimator.start();
                if (null != mOnActionListener) {
                        mOnActionListener.onPress();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                resetProgress();
                if (null != mOnActionListener) {
                    mOnActionListener.onLift();
                }
                break;
        }
        return true;
    }

    public void setCurrentProgress(float progress){
        this.currentProgress = progress;
        requestLayout();
        invalidate();
    }

    public void setCurrentText(String text){
        this.currentText = text;
    }

    public void resetProgress(){
        circleRadius = totalRadius - incrementRadius;
        currentProgress = 0;
        currentText = "";
        requestLayout();
        invalidate();
    }

    public interface OnActionListener{
        void onPress();
        void onLift();
    }

    OnActionListener mOnActionListener;

    public void setOnActionListener(OnActionListener onActionListener){
        this.mOnActionListener = onActionListener;
    }
}

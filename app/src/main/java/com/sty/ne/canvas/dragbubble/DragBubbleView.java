package com.sty.ne.canvas.dragbubble;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PointFEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by tian on 2019/10/14.
 */

public class DragBubbleView extends View {
    /**
     * 气泡默认状态-静止
     */
    private final int BUBBLE_STATE_DEFAULT = 0;
    /**
     * 气泡相连
     */
    private final int BUBBLE_STATE_CONNECT = 1;
    /**
     * 气泡分离
     */
    private final int BUBBLE_STATE_APART = 2;
    /**
     * 气泡消失
     */
    private final int BUBBLE_STATE_DISMISS = 3;
    /**
     * 气泡半径
     */
    private float mBubbleRadius;
    /**
     * 气泡半径Copy，用于恢复操作
     */
    private float mOldBubbleRadius;
    /**
     * 气泡颜色
     */
    private int mBubbleColor;
    /**
     * 气泡消息文字
     */
    private String mTextStr;
    /**
     * 气泡消息文字颜色
     */
    private int mTextColor;
    /**
     * 气泡消息文字大小
     */
    private float mTextSize;
    /**
     * 不动气泡的半径
     */
    private float mBubFixedRadius;
    /**
     * 可动气泡的半径
     */
    private float mBubMovableRadius;
    /**
     * 不动气泡的圆心
     */
    private PointF mBubFixedCenter;
    /**
     * 不动气泡圆心的copy，用于恢复操作
     */
    private PointF mOldBubFixedCenter;
    /**
     * 可动气泡的圆心
     */
    private PointF mBubMovableCenter;
    /**
     * 气泡的画笔
     */
    private Paint mBubblePaint;
    /**
     * 贝塞尔曲线path
     */
    private Path mBezierPath;

    /**
     * 气泡文字画笔
     */
    private Paint mTextPaint;
    //文本绘制区域
    private Rect mTextRect;
    //消失气泡画笔
    private Paint mBurstPaint;
    //爆炸区域绘制
    private Rect mBurstRect;

    /**
     * 气泡状态标志
     */
    private int mBubbleState = BUBBLE_STATE_DEFAULT;
    /**
     * 两气泡圆心距离
     */
    private float mDist;
    /**
     * 气泡相连状态最大圆心距离
     */
    private float mMaxDist;
    /**
     * 手指触摸偏移量
     */
    private final float MOVE_OFFSET;

    /**
     * 气泡爆炸的Bitmap数组
     */
    private Bitmap[] mBurstBitmapsArray;
    /**
     * 是否在执行气泡爆炸动画
     */
    private boolean mIsBurstAnimStart = false;

    /**
     * 当前气泡爆炸图片index
     */
    private int mCurDrawableIndex;

    /**
     * 气泡爆炸的图片id数据
     */
    private int[] mBurstDrawablesArray = {R.mipmap.burst_1, R.mipmap.burst_2, R.mipmap.burst_3,
            R.mipmap.burst_4, R.mipmap.burst_5};

    /**
     * 测试文字边界的画笔
     */
    private Paint mBorderPaint;

    public DragBubbleView(Context context) {
        this(context, null);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);


        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DragBubbleView, defStyleAttr, 0);
        mBubbleRadius = array.getDimension(R.styleable.DragBubbleView_bubble_radius, mBubbleRadius);
        mOldBubbleRadius = mBubbleRadius;
        mBubbleColor = array.getColor(R.styleable.DragBubbleView_bubble_color, Color.RED);
        mTextStr = array.getString(R.styleable.DragBubbleView_bubble_text);
        mTextSize = array.getDimension(R.styleable.DragBubbleView_bubble_textSize, mTextSize);
        mTextColor = array.getColor(R.styleable.DragBubbleView_bubble_textColor, Color.WHITE);
        array.recycle();

        //两个圆半径大小一致
        mBubFixedRadius = mBubbleRadius;
        mBubMovableRadius = mBubFixedRadius;
        mMaxDist = 8 * mBubbleRadius;

        MOVE_OFFSET = mMaxDist / 4;

        //抗锯齿
        mBubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBubblePaint.setColor(mBubbleColor);
        mBubblePaint.setStyle(Paint.Style.FILL);
        mBezierPath = new Path();

        //文本画笔
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
//        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextRect = new Rect();

        //爆炸画笔
        mBurstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBurstPaint.setFilterBitmap(true);
        mBurstRect = new Rect();
        mBurstBitmapsArray = new Bitmap[mBurstDrawablesArray.length];
        for (int i = 0; i < mBurstDrawablesArray.length; i++) {
            //将气泡爆炸的drawable转为bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mBurstDrawablesArray[i]);
            mBurstBitmapsArray[i] = bitmap;
        }

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.FILL);
//        mBorderPaint.setStrokeWidth(2);
        mBorderPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //不动气泡的圆心
        if (mBubFixedCenter == null) {
            mBubFixedCenter = new PointF(w / 2, h / 2);
            mOldBubFixedCenter = new PointF(w / 2, h / 2);
        } else {
            mBubFixedCenter.set(w / 2, h / 2);
            mOldBubFixedCenter.set(w / 2, h / 2);
        }

        //可动气泡的圆心
        if (mBubMovableCenter == null) {
            mBubMovableCenter = new PointF(w / 2, h / 2);
        } else {
            mBubMovableCenter.set(w / 2, h / 2);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //1.静止状态，一个气泡加消息数据
        //2.连接状态，一个气泡加消息数据，贝塞尔曲线，本身位置上气泡，大小可变化
        //3.分离状态，一个气泡加消息数据
        if (mBubbleState == BUBBLE_STATE_CONNECT) {
            //绘制不动气泡
            canvas.drawCircle(mBubFixedCenter.x, mBubFixedCenter.y, mBubFixedRadius, mBubblePaint);
            //绘制贝塞尔曲线 //参考show/bubble_drag_analyse.png
            //控制点坐标G
            int iAnchorX = (int) ((mBubFixedCenter.x + mBubMovableCenter.x) / 2);
            int iAnchorY = (int) ((mBubFixedCenter.y + mBubMovableCenter.y) / 2);

            //Theta = 角FBP = 角POE
            //sinTheta = PE / PO  //因为这里得到的是负值，所以下面涉及到的 + ，- 都取相反处理
            float sinTheta = (mBubMovableCenter.y - mBubFixedCenter.y) / mDist;
            //cosTheta = OE / PO
            float cosTheta = (mBubMovableCenter.x - mBubFixedCenter.x) / mDist;

            //B点坐标
            //P.x - PF
            float mBubMovableStartX = mBubMovableCenter.x + sinTheta * mBubMovableRadius;
            //P.y - BF
            float mBubMovableStartY = mBubMovableCenter.y - cosTheta * mBubMovableRadius;

            //A点坐标
            //O.x - OA * sinTheta
            float iBubFixedEndX = mBubFixedCenter.x + mBubFixedRadius * sinTheta;
            //O.y - OA * cosTheta
            float iBubFixedEndY = mBubFixedCenter.y - mBubFixedRadius * cosTheta;

            //D点坐标
            //O.x + OA * sinTheta
            float iBubFixedStartX = mBubFixedCenter.x - mBubFixedRadius * sinTheta;
            //O.y + OA * cosTheta
            float iBubFixedStartY = mBubFixedCenter.y + mBubFixedRadius * cosTheta;

            //C点坐标
            //P.x + PC * sinTheta
            float iBubMovableEndX = mBubMovableCenter.x - mBubMovableRadius * sinTheta;
            //P.y + PC * cosTheta
            float iBubMovableEndY = mBubMovableCenter.y + mBubMovableRadius * cosTheta;

            //贝塞尔曲线DC
            mBezierPath.reset();
            mBezierPath.moveTo(iBubFixedStartX, iBubFixedStartY);
            mBezierPath.quadTo(iAnchorX, iAnchorY, iBubMovableEndX, iBubMovableEndY);
            //移动到B点
            mBezierPath.lineTo(mBubMovableStartX, mBubMovableStartY);
            mBezierPath.quadTo(iAnchorX, iAnchorY, iBubFixedEndX, iBubFixedEndY);
            mBezierPath.close();

            canvas.drawPath(mBezierPath, mBubblePaint);

        }

        //静止，连接，分离状态都需要绘制圆背景以及文本
        if (mBubbleState != BUBBLE_STATE_DISMISS) {
            //绘制一个小球加消息数据
            canvas.drawCircle(mBubMovableCenter.x, mBubMovableCenter.y, mBubMovableRadius, mBubblePaint);
            mTextPaint.getTextBounds(mTextStr, 0, mTextStr.length(), mTextRect); //mTextRect(3, -26, 39, 0)
            canvas.drawText(mTextStr, mBubMovableCenter.x - mTextRect.width() / 2, //36/2 = 18
                    mBubMovableCenter.y + mTextRect.height() / 2, mTextPaint); //26/2=13

            //mTextRect(3, -26, 39, 0) 文本的矩形框默认向右偏移了3个像素，导致文本不居中，坐标中心点相减的算法则避免了这个绘制问题
            canvas.drawText(mTextStr, mBubMovableCenter.x - mTextRect.centerX(), //centerX:21 centerY:-13
                    mBubMovableCenter.y - mTextRect.centerY(), mTextPaint); //mBubMovableCenter.x:540 mBubMovableCenter.y:720
        }

        //4.消失状态，爆炸效果
        if (mBubbleState == BUBBLE_STATE_DISMISS && mCurDrawableIndex < mBurstBitmapsArray.length) {
            mBurstRect.set(
                    (int) (mBubMovableCenter.x - mBubMovableRadius),
                    (int) (mBubMovableCenter.y - mBubMovableRadius),
                    (int) (mBubMovableCenter.x + mBubMovableRadius),
                    (int) (mBubMovableCenter.y + mBubMovableRadius)
            );
            canvas.drawBitmap(mBurstBitmapsArray[mCurDrawableIndex], null, mBurstRect, mBubblePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mBubbleState != BUBBLE_STATE_DISMISS) {
                    mDist = (float) Math.hypot(event.getX() - mBubFixedCenter.x, event.getY() - mBubFixedCenter.y);
                    if (mDist < mBubbleRadius + MOVE_OFFSET) { //加上MOVE_OFFSET是为了方便拖拽
                        mBubbleState = BUBBLE_STATE_CONNECT;
                    } else {
                        mBubbleState = BUBBLE_STATE_DEFAULT;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mBubbleState != BUBBLE_STATE_DEFAULT) {
                    mDist = (float) Math.hypot(event.getX() - mBubFixedCenter.x, event.getY() - mBubFixedCenter.y);
                    mBubMovableCenter.x = event.getX();
                    mBubMovableCenter.y = event.getY();
                    if (mBubbleState == BUBBLE_STATE_CONNECT) {
                        if (mDist < mMaxDist - MOVE_OFFSET) { //当拖拽的距离在指定范围内，那么调整不动圆的半径
                            mBubFixedRadius = mBubbleRadius - mDist / 8;
                        } else {  //当拖拽的距离超过指定范围，那么改成分离状态
                            mBubbleState = BUBBLE_STATE_APART;
                        }
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mBubbleState == BUBBLE_STATE_CONNECT) {
                    //橡皮筋动画效果
                    startBubbleRestAnim();
                } else if (mBubbleState == BUBBLE_STATE_APART) {
                    if (mDist < 2 * mBubbleRadius) {
                        startBubbleRestAnim();
                    } else {
                        //爆炸动画
                        startBubbleBurstAnim();
                    }
                }
                break;
            default:
                break;

        }
        return true;
    }

    /**
     * 连接状态下松开手指，执行类似橡皮筋动画
     */
    private void startBubbleRestAnim() {
        ValueAnimator anim = ValueAnimator.ofObject(new PointFEvaluator(),
                new PointF(mBubMovableCenter.x, mBubMovableCenter.y),
                new PointF(mBubFixedCenter.x, mBubFixedCenter.y));
        anim.setDuration(200);
        anim.setInterpolator(new OvershootInterpolator(5f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBubMovableCenter = (PointF) animation.getAnimatedValue();
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mBubbleState = BUBBLE_STATE_DEFAULT;
            }
        });
        anim.start();
    }

    /**
     * 爆炸动画
     */
    private void startBubbleBurstAnim() {
        mBubbleState = BUBBLE_STATE_DISMISS;
        ValueAnimator anim = ValueAnimator.ofInt(0, mBurstBitmapsArray.length);
        anim.setDuration(500);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurDrawableIndex = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        anim.start();
    }

    public void reset() {
        mBubbleState = BUBBLE_STATE_DEFAULT;
        mBubFixedCenter.set(mOldBubFixedCenter.x, mOldBubFixedCenter.y);
        mBubMovableCenter.set(mOldBubFixedCenter.x, mOldBubFixedCenter.y);
        mBubFixedRadius = mOldBubbleRadius;
        mBubMovableRadius = mOldBubbleRadius;
        invalidate();
    }
}

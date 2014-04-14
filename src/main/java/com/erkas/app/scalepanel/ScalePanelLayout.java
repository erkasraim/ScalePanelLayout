package com.erkas.app.scalepanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v4.widget.ViewDragHelperCustom;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;


public class ScalePanelLayout extends ViewGroup {

    private static final String TAG = ScalePanelLayout.class.getSimpleName();

    /**
     * Default peeking out panel height
     */
    private static final int DEFAULT_PANEL_WIDTH = 68; // dp;

    /**
     * Default peeking out panel height
     */
    private static final int DEFAULT_PANEL_HEIGHT = 68; // dp;

    /**
     * If no fade color is given by default it will fade to 80% gray.
     */
    private static final int DEFAULT_FADE_COLOR = 0x99000000;

    /**
     * Default Minimum velocity that will be detected as a fling
     */
    private static final int DEFAULT_MIN_FLING_VELOCITY = 400; // dips per second

    /**
     * Default attributes for layout
     */
    private static final int[] DEFAULT_ATTRS = new int[] {
            android.R.attr.gravity
    };
    private boolean mIsGravityRight;

    /**
     * Use Expand View Width when sliding
     */
    private boolean mUseExpandView = false;

    /**
     * Expand View Res Id
     */
    private int mExpandViewResId = -1;

    /**
     * Expand View when sliding
     */
    private View mExpandView;

    /**
     * Expand View min width
     */
    private int mExpandMinWidth = -1;

    /**
     * Expand View min height
     */
    private int mExpandMinHeight = -1;

    /**
     * Minimum velocity that will be detected as a fling
     */
    private int mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY;

    /**
     * The fade color used for the panel covered by the slider. 0 = no fading.
     */
    private int mCoveredFadeColor = DEFAULT_FADE_COLOR;

    /**
     * The paint used to dim the main layout when sliding
     */
    private final Paint mCoveredFadePaint = new Paint();


    /**
     * The size of the overhang in pixels.
     */
    private int mPanelWidth = -1;

    /**
     * The size of the overhang in pixels.
     */
    private int mPanelHeight = -1;

    /**
     * True if the collapsed panel should be dragged up/down.
     */
    private boolean mIsExpanding;

    /**
     * True if the collapsed panel should be dragged up.
     */
    private boolean mIsGravityBottom;

    /**
     * True if a panel can scale change with the current measurements
     */
    private boolean mCanScaleChange;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private View mDragView;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private int mDragViewResId = -1;

    /**
     * The child view that can scale, if any.
     */
    private View mScaleableView;

    /**
     * Current state of the scaleable view.
     */
    private enum ScaleState {
        EXPANDED,
        COLLAPSED
    }
    private ScaleState mScaleState = ScaleState.COLLAPSED;

    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = expanded, 1 = collapsed.
     */
    private float mScaleOffset;

    /**
     * How far in pixels the scaleable panel may move.
     *
     * collapse -> expand 일때 뷰의 크기 변화 값.
     */
    private int mScaleRangeY;
    private int mScaleRangeX;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     *
     * 드래그 상태에 대한 조건 값. true 이면 드래그 상태이다.
     */
    private boolean mIsUnableToDrag;

    /**
     * Flag indicating that scale feature is enabled\disabled
     */
    private boolean mIsScaleEnabled;

    /**
     * 컨텐츠 뷰를 포함하는 경우를 대비한 값.
     */
    private boolean mHasContentView;

    /**
     * Flag indicating if a drag view can have its own touch events.  If set
     * to true, a drag view can scroll horizontally and have its own click listener.
     *
     * Default is set to false.
     *
     * true 로 설정하면 dragView 내부의 클릭이벤트를 잡아먹지 않는다.
     */
    private boolean mIsUsingDragViewTouchEvents;

    /**
     * Threshold to tell if there was a scroll touch event.
     */
    private final int mScrollTouchSlop;

    private float mInitialMotionX;
    private float mInitialMotionY;

    private PanelScaleListener mPanelScaleListener;

    private final ViewDragHelperCustom mDragHelper;

    /**
     * Stores whether or not the pane was expanded the last time it was scaleable.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mFirstLayout = true;

    private final Rect mTmpRect = new Rect();

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelScaleListener {
        /**
         * Called when a sliding pane's position changes.
         * @param panel The child view that was moved
         * @param scaleOffset The new offset of this scale pane within its range, from 0-1 (expand - collapse)
         */
        public void onPanelScale(View panel, float scaleOffset);
        /**
         * Called when a scale pane becomes scale completely collapsed(offset 1). The pane may or may not
         * be interactive at this point depending on if it's shown or hidden
         * @param panel The child view that was slid to an collapsed position, revealing other panes
         */
        public void onPanelCollapsed(View panel);

        /**
         * Called when a scale pane becomes scale completely expanded(offset 1). The pane is now guaranteed
         * to be interactive. It may now obscure other views in the layout.
         * @param panel The child view that was scale to a expanded position
         */
        public void onPanelExpanded(View panel);
    }

    /**
     * No-op stubs for {@link ScalePanelLayout.PanelScaleListener}. If you only want to implement a subset
     * of the listener methods you can extend this instead of implement the full interface.
     */
    public static class SimplePanelScaleListener implements PanelScaleListener {
        @Override
        public void onPanelScale(View panel, float slideOffset) {
        }
        @Override
        public void onPanelCollapsed(View panel) {
        }
        @Override
        public void onPanelExpanded(View panel) {
        }
    }

    public ScalePanelLayout(Context context) {
        this(context, null);
    }

    public ScalePanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalePanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);

            if (defAttrs != null) {
                int mGravity = defAttrs.getInt(0, Gravity.NO_GRAVITY);

                final int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
                if (majorGravity != Gravity.TOP && majorGravity != Gravity.BOTTOM) {
                    throw new IllegalArgumentException("layout_gravity must be set to either top or bottom");
                }

                final int layoutDirection = ViewCompat.getLayoutDirection(this);
                final int absoluteGravity = GravityCompat.getAbsoluteGravity(mGravity, layoutDirection); // for RTL, LTR
                final int minorGravity = absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                mIsExpanding = true;
                mIsGravityBottom = majorGravity == Gravity.BOTTOM;
                mIsGravityRight = minorGravity == Gravity.RIGHT;

                defAttrs.recycle();
            }

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScalePanelLayout);

            if (ta != null) {
                mPanelWidth = ta.getDimensionPixelSize(R.styleable.ScalePanelLayout_collapsedWidth, -1);
                mPanelHeight = ta.getDimensionPixelSize(R.styleable.ScalePanelLayout_collapsedHeight, -1);

                mMinFlingVelocity = ta.getInt(R.styleable.ScalePanelLayout_flingVelocity, DEFAULT_MIN_FLING_VELOCITY);
                mCoveredFadeColor = ta.getColor(R.styleable.ScalePanelLayout_fadeColor, DEFAULT_FADE_COLOR);

                mDragViewResId = ta.getResourceId(R.styleable.ScalePanelLayout_dragView, -1);
                mExpandViewResId = ta.getResourceId(R.styleable.ScalePanelLayout_expandView, -1);

                ta.recycle();
            }
        }

        final float density = context.getResources().getDisplayMetrics().density;
        if (mPanelHeight == -1) {
            mPanelHeight = (int) (DEFAULT_PANEL_HEIGHT * density + 0.5f);
        }
        if (mPanelWidth == -1) {
            mPanelWidth = (int) (DEFAULT_PANEL_WIDTH * density + 0.5f);
        }

        setWillNotDraw(false);

        mDragHelper = ViewDragHelperCustom.create(this, 0.5f, new DragHelperCallback());
        mDragHelper.setMinVelocity(mMinFlingVelocity * density);

        mCanScaleChange = true;
        mIsScaleEnabled = true;

        ViewConfiguration vc = ViewConfiguration.get(context);
        mScrollTouchSlop = vc.getScaledTouchSlop();
    }

    /**
     * Set the Drag View after the view is inflated
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDragViewResId != -1) {
            mDragView = findViewById(mDragViewResId);
        }

        if (mExpandViewResId != -1) {
            mExpandView = findViewById(mExpandViewResId);
            mUseExpandView = true;
        }
    }

    /**
     * Set the color used to fade the pane covered by the sliding pane out when the pane
     * will become fully covered in the expanded state.
     *
     * @param color An ARGB-packed color value
     */
    public void setCoveredFadeColor(int color) {
        mCoveredFadeColor = color;
        invalidate();
    }

    /**
     * @return The ARGB-packed color value used to fade the fixed pane
     */
    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }

    /**
     * Set the collapsed panel width in pixels
     *
     * @param val A width in pixels
     */
    public void setPanelWidth(int val) {
        mPanelWidth = val;
        requestLayout();
    }

    /**
     * Set the collapsed panel height in pixels
     *
     * @param val A height in pixels
     */
    public void setPanelHeight(int val) {
        mPanelHeight = val;
        requestLayout();
    }

    /**
     * @return The current collapsed panel height
     */
    public int getPanelWidth() {
        return mPanelWidth;
    }

    /**
     * @return The current collapsed panel height
     */
    public int getPanelHeight() {
        return mPanelHeight;
    }

    public void setPanelScaleListener(PanelScaleListener listener) {
        mPanelScaleListener = listener;
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragView A view that will be used to drag the panel.
     */
    public void setDragView(View dragView) {
        mDragView = dragView;
    }


    void dispatchOnPanelScale(View panel) {
        if (mPanelScaleListener != null) {
            mPanelScaleListener.onPanelScale(panel, mScaleOffset);
        }
    }

    void dispatchOnPanelExpanded(View panel) {
        if (mPanelScaleListener != null) {
            mPanelScaleListener.onPanelExpanded(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelCollapsed(View panel) {
        if (mPanelScaleListener != null) {
            mPanelScaleListener.onPanelCollapsed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewVisibility() {
        if (getChildCount() == 0) {
            return;
        }
        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (mScaleableView != null && hasOpaqueBackground(mScaleableView)) {
            left = mScaleableView.getLeft();
            right = mScaleableView.getRight();
            top = mScaleableView.getTop();
            bottom = mScaleableView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }
        View child = getChildAt(0);
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int vis;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            vis = INVISIBLE;
        } else {
            vis = VISIBLE;
        }
        child.setVisibility(vis);
    }

    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();

        final int childCount = getChildCount();

        if (childCount > 1) {
            Log.e(TAG, "onMeasure: More than two child views are not supported.");
        }

        // First pass. Measure based on child LayoutParams width/height.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(0);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            lp.scaleable = true;
            lp.dimWhenOffset = true;
            mScaleableView = child;
            mCanScaleChange = true;

            int childWidthSpec;
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(layoutHeight, MeasureSpec.AT_MOST);
            } else if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(layoutHeight, MeasureSpec.EXACTLY);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }

//            Log.d("onMeasure", "child width : " + lp.width + ", height : " + lp.height);
            child.measure(childWidthSpec, childHeightSpec);
        }

        if (mUseExpandView && mExpandMinWidth < 0) {
            ViewGroup.LayoutParams expandViewlp = mExpandView.getLayoutParams();
            if (expandViewlp.width == LayoutParams.WRAP_CONTENT) {
                mExpandMinWidth = mExpandView.getMeasuredWidth();
            } else if (expandViewlp.width == LayoutParams.MATCH_PARENT) {
                throw new IllegalStateException("Expand View Width must have an exact value or WRAP_CONTENT");
            } else {
                mExpandMinWidth = expandViewlp.width;
            }

            if (expandViewlp.height == LayoutParams.WRAP_CONTENT) {
                mExpandMinHeight = mExpandView.getMeasuredHeight();
            } else if (expandViewlp.height == LayoutParams.MATCH_PARENT) {
                throw new IllegalStateException("Expand View Width must have an exact value or WRAP_CONTENT");
            } else {
                mExpandMinHeight = expandViewlp.height;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int slidingTop = getSlidingTop(); // collapse : getMeasuredHeight() - getPaddingBottom() - (mScaleableView.getMeasuredHeight());

//        Log.d("onLayout", String.format("paddingLeft : %d, paddingTop : %d, slidingTop : %d", paddingLeft, paddingTop, slidingTop));

        final int childCount = getChildCount();

        if (mFirstLayout) {
            switch (mScaleState) {
                case EXPANDED:
                    mScaleOffset = mCanScaleChange ? 0.f : 1.f;
                    break;
                default:
                    mScaleOffset = 1.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final int childHeight = child.getMeasuredHeight();
            final int childWidth = child.getMeasuredWidth();

            if (lp.scaleable) {
                mScaleRangeY = getMeasuredHeight() - mPanelHeight;
                mScaleRangeX = getMeasuredWidth() - mPanelWidth;
            }

//            Log.d("onLayout", String.format("mScaleRangeY : %d, childHeight : %d, mPanelHeight : %d", mScaleRangeY, childHeight, mPanelHeight));

            final int childTop;
            final int childLeft;
            if (mIsGravityBottom) {
                childTop = lp.scaleable ? (int) (mScaleRangeY * mScaleOffset) : paddingTop;
            } else {
                childTop = lp.scaleable ? slidingTop - (int) (mScaleRangeY * mScaleOffset) : paddingTop + mPanelHeight;
            }

            if (mIsGravityRight) {
                childLeft = getMeasuredWidth() - getPaddingRight() - child.getMeasuredWidth() - lp.rightMargin;
            } else {
                childLeft = getPaddingLeft() + lp.leftMargin;
            }

            final int childBottom = childTop + childHeight;
            final int childRight = childLeft + childWidth;

//            Log.d("onLayout", String.format("childLeft : %d, childTop : %d, childRight : %d, childBottom : %d", childLeft, childTop, childRight, childBottom));

            child.layout(childLeft, childTop, childRight, childBottom);
        }

        if (mFirstLayout) {
            updateObscuredViewVisibility();
        }

        mFirstLayout = false;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
//        Log.e("debug", "onSizeChanged");
        if (h != oldh) {
            mFirstLayout = true;
        }
    }

    /**
     * Set sliding enabled flag
     * @param enabled flag value
     */
    public void setSlidingEnabled(boolean enabled) {
        mIsScaleEnabled = enabled;
    }

    /**
     * Set if the drag view can have its own touch events.  If set
     * to true, a drag view can scroll horizontally and have its own click listener.
     *
     * Default is set to false.
     */
    public void setEnableDragViewTouchEvents(boolean enabled) {
        mIsUsingDragViewTouchEvents = enabled;
    }

    /**
     *
     * @param ev
     * @return true : comsume touch event, otherwise false
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (!mCanScaleChange || !mIsScaleEnabled || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        final float x = ev.getX();
        final float y = ev.getY();
        boolean interceptTap = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (isDragViewUnder((int) x, (int) y) && !mIsUsingDragViewTouchEvents) {
                    interceptTap = true;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(y - mInitialMotionY);
                final int dragSlop = mDragHelper.getTouchSlop();

                // Handle any horizontal scrolling on the drag view.
                if (mIsUsingDragViewTouchEvents) {
                    if (adx > mScrollTouchSlop && ady < mScrollTouchSlop) {
                        return super.onInterceptTouchEvent(ev);
                    }
                    // Intercept the touch if the drag view has any vertical scroll.
                    // onTouchEvent will determine if the view should drag vertically.
                    else if (ady > mScrollTouchSlop) {
                        interceptTap = isDragViewUnder((int) x, (int) y);
                    }
                    else if (adx > mScrollTouchSlop) {
                        // should intercept drag horizontally
                    }
                }

                if ((ady > dragSlop && adx > ady) || !isDragViewUnder((int) x, (int) y)) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }
        }

        final boolean interceptForDrag = mDragHelper.shouldInterceptTouchEvent(ev);

        return interceptForDrag || interceptTap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mCanScaleChange || !mIsScaleEnabled) {
            return super.onTouchEvent(ev);
        }

        final float x = ev.getX();
        final float y = ev.getY();

        if (mDragHelper.getViewDragState() != ViewDragHelper.STATE_DRAGGING
                && !mHasContentView && !isDragViewUnder((int) x, (int) y)) {
            return super.onTouchEvent(ev);
        }

        mDragHelper.processTouchEvent(ev);

        final int action = ev.getAction();
        boolean wantTouchEvents = true;

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                final float dx = x - mInitialMotionX;
                final float dy = y - mInitialMotionY;
                final int slop = mDragHelper.getTouchSlop();
                View dragView = mDragView != null ? mDragView : mScaleableView;
                if (dx * dx + dy * dy < slop * slop &&
                        isDragViewUnder((int) x, (int) y)) {
                    dragView.playSoundEffect(SoundEffectConstants.CLICK);
                    if (!isExpanded()) {
                        expandPane(0.f);
                    } else {
                        collapsePane();
                    }
                    break;
                }
                break;
            }
        }

        return wantTouchEvents;
    }

    // 터치 좌표가 dragView 안에서 발생했는지 확인하는 함수.
    private boolean isDragViewUnder(int x, int y) {
        View dragView = mDragView != null ? mDragView : mScaleableView;
        if (dragView == null) return false;
        int[] viewLocation = new int[2];
        dragView.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + dragView.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + dragView.getHeight();
    }

    private boolean expandPane(View pane, int initialVelocity, float mSlideOffset) {
        if (mFirstLayout || smoothSlideTo(mSlideOffset, initialVelocity)) {
            return true;
        }
        return false;
    }

    private boolean collapsePane(View pane, int initialVelocity) {
        if (mFirstLayout || smoothSlideTo(1.f, initialVelocity)) {
            return true;
        }
        return false;
    }

    private int getSlidingTop() {
        if (mScaleableView != null) {
            return mIsExpanding
                    ? getMeasuredHeight() - getPaddingBottom() - mScaleableView.getMeasuredHeight()
                    : getMeasuredHeight() - getPaddingBottom() - (mScaleableView.getMeasuredHeight() * 2);
        }

        return getMeasuredHeight() - getPaddingBottom();
    }

    /**
     * Collapse the sliding pane if it is currently scaleable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was scaleable and is now collapsed/in the process of collapsing
     */
    public boolean collapsePane() {
        return collapsePane(mScaleableView, 0);
    }

    /**
     * Expand the sliding pane if it is currently scaleable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was scaleable and is now expanded/in the process of expading
     */
    public boolean expandPane() {
        return expandPane(0);
    }

    /**
     * Partially expand the sliding pane up to a specific offset
     *
     * @param mSlideOffset Value between 0 and 1, where 0 is completely expanded.
     * @return true if the pane was scaleable and is now expanded/in the process of expading
     */
    public boolean expandPane(float mSlideOffset) {
        if (!isPaneVisible()) {
            showPane();
        }
        return expandPane(mScaleableView, 0, mSlideOffset);
    }

    /**
     * Check if the layout is completely expanded.
     *
     * @return true if sliding panels are completely expanded
     */
    public boolean isExpanded() {
        return mScaleState == ScaleState.EXPANDED;
    }

    /**
     * Check if the content in this layout cannot fully fit side by side and therefore
     * the content pane can be slid back and forth.
     *
     * @return true if content in this layout can be expanded
     */
    public boolean isSlideable() {
        return mCanScaleChange;
    }

    public boolean isPaneVisible() {
        if (getChildCount() < 2) {
            return false;
        }
        View slidingPane = getChildAt(1);
        return slidingPane.getVisibility() == View.VISIBLE;
    }

    public void showPane() {
        if (getChildCount() < 2) {
            return;
        }
        View slidingPane = getChildAt(1);
        slidingPane.setVisibility(View.VISIBLE);
        requestLayout();
    }

    public void hidePane() {
        if (mScaleableView == null) {
            return;
        }
        mScaleableView.setVisibility(View.GONE);
        requestLayout();
    }

    private void onPanelDragged(int newTop) {
        final int topBound = 0;//getSlidingTop();
        if (newTop == 1) {
            newTop = 0;
        }
        mScaleOffset = mIsGravityBottom
                ? (float) (newTop) / mScaleRangeY
                : 1.0f - (float) (newTop) / mScaleRangeY;
//        Log.d("onPanelDragged", "mScaleOffset : " + mScaleOffset + ", newTop : " + newTop + ", topBound : " + topBound);
        dispatchOnPanelScale(mScaleableView);
    }

    private void onExpandViewResize() {
        if (mUseExpandView && mExpandView != null) {
            int widthSize = getMeasuredWidth();
            int heightSize = getMeasuredHeight();
            ViewGroup.LayoutParams params = mExpandView.getLayoutParams();

            params.width = (int)((1.0f - mScaleOffset) * (widthSize - mExpandMinWidth)) + mExpandMinWidth;
            params.height = (int)((1.0f - mScaleOffset) * (heightSize - mExpandMinHeight)) + mExpandMinHeight;

//            Log.d("onExpandViewResize", String.format("width : %d, height : %d, mScaleOffset : %f, widthSize : %d, mExpandMinWidth : %d"
//                    , params.width, params.height, mScaleOffset, widthSize, mExpandMinWidth));

            mExpandView.setLayoutParams(params);
        }

    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        boolean result;
        final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);

        boolean drawScrim = false;

        if (mCanScaleChange && !lp.scaleable && mScaleableView != null) {
            // Clip against the slider; no sense drawing what will immediately be covered.
            canvas.getClipBounds(mTmpRect);
            if (!mUseExpandView) {
                if (mIsExpanding) {
                    mTmpRect.bottom = Math.min(mTmpRect.bottom, mScaleableView.getTop());
                } else {
                    mTmpRect.top = Math.max(mTmpRect.top, mScaleableView.getBottom());
                }
            }

            canvas.clipRect(mTmpRect);
            if (mScaleOffset < 1) {
                drawScrim = true;
            }
        }

        result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(save);

        if (drawScrim) {
            final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;
            final int imag = (int) (baseAlpha * (1 - mScaleOffset));
            final int color = imag << 24 | (mCoveredFadeColor & 0xffffff);
            mCoveredFadePaint.setColor(color);
            canvas.drawRect(mTmpRect, mCoveredFadePaint);
        }

        return result;
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity initial velocity in case of fling, or 0.
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        if (!mCanScaleChange) {
            // Nothing to do.
            return false;
        }

        int x = mIsGravityRight
                ? (int) (getPaddingLeft() + slideOffset * mScaleRangeX)
                : 0;

        final int topBound = getPaddingTop(); //getSlidingTop();
        int y = mIsGravityBottom
                ? (int) (topBound + slideOffset * mScaleRangeY)
                : (int) (getMeasuredHeight() * (1.f - slideOffset) - topBound);

//        Log.e("smoothSlideTo", "x : " + x + ", y : " + y + ", slideOffset : " + slideOffset);

        if (mDragHelper.smoothSlideViewTo(mScaleableView, x, y, !mIsGravityBottom)) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true, mIsGravityBottom)) {
            if (!mCanScaleChange) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        // should draw extra views
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx Delta scrolled in pixels
     * @param x X coordinate of the active touch point
     * @param y Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
                        y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
                        canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && ViewCompat.canScrollHorizontally(v, -dx);
    }


    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.mScaleState = mScaleState;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mScaleState = ss.mScaleState;
    }

    private class DragHelperCallback extends ViewDragHelperCustom.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
//            Log.e("tryCaptureView", "child width : " + child.getMeasuredWidth() + ", height : " + child.getMeasuredHeight());
            if (mIsUnableToDrag) {
                return false;
            }

            return ((LayoutParams) child.getLayoutParams()).scaleable;
        }

        @Override
        public void onViewDragStateChanged(int state) {
//            Log.e("onViewDragStateChanged", "state : " + state + ", mScaleOffset : " + mScaleOffset);
            if (state == ViewDragHelperCustom.STATE_IDLE) {
                if (mScaleOffset == 0) {
                    if (mScaleState != ScaleState.EXPANDED) {
                        updateObscuredViewVisibility();
                        dispatchOnPanelExpanded(mScaleableView);
                        mScaleState = ScaleState.EXPANDED;
                    }

                } else if (mScaleState != ScaleState.COLLAPSED) {
                    dispatchOnPanelCollapsed(mScaleableView);
                    mScaleState = ScaleState.COLLAPSED;
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
//            Log.e("onViewCaptured", "child width : " + capturedChild.getMeasuredWidth() + ", height : " + capturedChild.getMeasuredHeight());
            // Make all child views visible in preparation for sliding things around
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
//            Log.e("onViewPositionChanged", "left : " + left + ", top : " + top + ", dx : " + dx + ", dy : " + dy);
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING) {
                if (!mIsGravityBottom && top > mScaleRangeY) {
                    // expand
                    top -= mExpandMinHeight;
                }
            }
            onPanelDragged(top);
            onExpandViewResize();
            invalidate();
        }

        // touch Up 이벤트시에 뷰 위치 계산.
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top = getPaddingTop();
            int left = getPaddingLeft();

            boolean isExpand = false;
            if (mIsGravityBottom) {
                if (yvel < 0 || (yvel == 0 && mScaleOffset < 0.5f)) {
                    // expand
                    isExpand = true;
                }
            } else {
                if (yvel > 0 || (yvel == 0 && mScaleOffset < 0.5f)) {
                    // expand
                    isExpand = true;
                }
            }


            if (mIsGravityRight) {
                if (!isExpand) {
                    left += mScaleRangeX;
                }
            }

            if (mIsGravityBottom) {
                if (!isExpand) {
                    top += mScaleRangeY;
                }
            } else {
                if (isExpand) {
                    top = getMeasuredHeight() - top;
                }
            }

//            Log.e("onViewReleased", "releasedChild.getLeft() : " + left + ", top : " + top + ", yvel : " + yvel);
            mDragHelper.settleCapturedViewAt(left, top, !mIsGravityBottom);
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mScaleRangeY;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            // 가로 확장 지원시 값을 주어야 함.
            return mScaleRangeX;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound;
            final int bottomBound;
//            Log.e("clampViewPositionVertical", "top : " + top + ", dy : " + dy);
            if (mIsGravityBottom) {
                topBound = getPaddingTop();//getSlidingTop();
                bottomBound = topBound + mScaleRangeY;
                return Math.min(Math.max(top, topBound), bottomBound);
            } else {
                topBound = getPaddingTop();//getSlidingTop();
                bottomBound = topBound + mScaleRangeY;
                int height = child.getMeasuredHeight() + dy - mExpandMinHeight;
                return Math.min(Math.max(height, topBound), bottomBound);
            }
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final int leftBound;
            final int rightBound;
            if (mIsGravityRight) {
                leftBound = getPaddingLeft();
                rightBound = leftBound + mScaleRangeX;
            } else {
                return getPaddingLeft();
            }

            // 좌우 이동을 위해서는 return 값을 조절해야 한다.
            return Math.min(Math.max(left, leftBound), rightBound);
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        private static final int[] ATTRS = new int[] {
                android.R.attr.layout_weight
        };

        /**
         * True if this pane is the scaleable pane in the layout.
         */
        boolean scaleable;

        /**
         * True if this view should be drawn dimmed
         * when it's been offset from its default position.
         */
        boolean dimWhenOffset;

        Paint dimPaint;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            a.recycle();
        }

    }

    static class SavedState extends BaseSavedState {
        ScaleState mScaleState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            try {
                mScaleState = Enum.valueOf(ScaleState.class, in.readString());
            } catch (IllegalArgumentException e) {
                mScaleState = ScaleState.COLLAPSED;
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(mScaleState.toString());
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}

package com.ramotion.foldingcell;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.ramotion.foldingcell.animations.AnimationEndListener;
import com.ramotion.foldingcell.animations.FoldAnimation;
import com.ramotion.foldingcell.animations.HeightAnimation;
import com.ramotion.foldingcell.views.FoldingCellView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Very first implementation of Folding Cell by Ramotion for Android platform
 */
public class FoldingCell extends RelativeLayout {

    private final String TAG = "folding-cell";

    // state variables
    private boolean mUnfolded;
    private boolean mAnimationInProgress;

    // default values
    private final int DEF_ANIMATION_DURATION = 1000;
    private final int DEF_BACK_SIDE_COLOR = Color.GRAY;

    // current settings
    private int mAnimationDuration = DEF_ANIMATION_DURATION;
    private int mBackSideColor = DEF_BACK_SIDE_COLOR;

    public FoldingCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeFromAttributes(context, attrs);
        this.setClipChildren(false);
        this.setClipToPadding(false);
    }

    public FoldingCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeFromAttributes(context, attrs);
        this.setClipChildren(false);
        this.setClipToPadding(false);
    }

    public FoldingCell(Context context) {
        super(context);
        this.setClipChildren(false);
        this.setClipToPadding(false);
    }

    /**
     * Initializes folding cell programmatically with custom settings
     *
     * @param animationDuration animation duration, default is 1000
     * @param backSideColor     color of back side, default is android.graphics.Color.GREY (0xFF888888)
     */
    public void initialize(int animationDuration, int backSideColor) {
        this.mAnimationDuration = animationDuration;
        this.mBackSideColor = backSideColor;
    }

    /**
     * Unfold cell with (or without) animation
     *
     * @param skipAnimation if true - change state of cell instantly without animation
     */
    public void unfold(boolean skipAnimation) {
        if (mUnfolded || mAnimationInProgress) return;

        if (skipAnimation) {
            setStateToUnfolded();
            return;
        }

        // get main content parts
        final View contentView = getChildAt(0);
        if (contentView == null) return;
        final View titleView = getChildAt(1);
        if (titleView == null) return;

        // create layout container for animation elements
        final LinearLayout foldingLayout = createAndPrepareFoldingContainer();
        this.addView(foldingLayout);

        // hide title and content views
        titleView.setVisibility(GONE);
        contentView.setVisibility(GONE);

        // take bitmaps from title and content views
        Bitmap bitmapFromTitleView = getBitmapFromView(titleView, this.getMeasuredWidth());
        Bitmap bitmapFromContentView = getBitmapFromView(contentView, this.getMeasuredWidth());

        // create list with animation parts for animation
        ArrayList<FoldingCellView> foldingCellElements = prepareViewsForAnimation(bitmapFromTitleView, bitmapFromContentView);

        // start fold animation with end listener
        int childCount = foldingCellElements.size();
        int view90degreeAnimationDuration = mAnimationDuration / (childCount * 2);

        startUnfoldAnimation(foldingCellElements, foldingLayout, view90degreeAnimationDuration, new AnimationEndListener() {
            public void onAnimationEnd(Animation animation) {
                contentView.setVisibility(VISIBLE);
                foldingLayout.setVisibility(GONE);
                FoldingCell.this.removeView(foldingLayout);
                FoldingCell.this.mUnfolded = true;
                FoldingCell.this.mAnimationInProgress = false;
            }
        });

        int height = titleView.getHeight();
        int smallHeight = bitmapFromContentView.getHeight() % height;
        startExpandHeightAnimation(childCount, view90degreeAnimationDuration * 2, height, smallHeight);
        this.mAnimationInProgress = true;
    }

    /**
     * Fold cell with (or without) animation
     *
     * @param skipAnimation if true - change state of cell instantly without animation
     */
    public void fold(boolean skipAnimation) {
        if (!mUnfolded || mAnimationInProgress) return;
        if (skipAnimation) {
            setStateToFolded();
            return;
        }

        // get basic views
        final View contentView = getChildAt(0);
        if (contentView == null) return;
        final View titleView = getChildAt(1);
        if (titleView == null) return;

        // create empty layout for folding animation
        final LinearLayout foldingLayout = createAndPrepareFoldingContainer();
        // add that layout to structure
        this.addView(foldingLayout);

        // make bitmaps from title and content views
        Bitmap bitmapFromTitleView = getBitmapFromView(titleView, this.getMeasuredWidth());
        Bitmap bitmapFromContentView = getBitmapFromView(contentView, this.getMeasuredWidth());

        // hide title and content views
        titleView.setVisibility(GONE);
        contentView.setVisibility(GONE);

        // create views list with bitmap parts for fold animation
        ArrayList<FoldingCellView> foldingCellElements = prepareViewsForAnimation(bitmapFromTitleView, bitmapFromContentView);

        int childCount = foldingCellElements.size();
        int part90degreeAnimationDuration = mAnimationDuration / (childCount * 2);

        // start fold animation with end listener
        startFoldAnimation(foldingCellElements, foldingLayout, part90degreeAnimationDuration, new AnimationEndListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                contentView.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                foldingLayout.setVisibility(GONE);
                FoldingCell.this.removeView(foldingLayout);
                FoldingCell.this.mAnimationInProgress = false;
                FoldingCell.this.mUnfolded = false;
            }
        });

        int height = titleView.getHeight();
        int smallHeight = bitmapFromContentView.getHeight() % height;
        startCollapseHeightAnimation(childCount, part90degreeAnimationDuration * 2, height, smallHeight);
        this.mAnimationInProgress = true;
    }


    /**
     * Toggle current state of FoldingCellLayout
     */
    public void toggle(boolean skipAnimation) {
        if (this.mUnfolded) {
            this.fold(skipAnimation);
        } else {
            this.unfold(skipAnimation);
        }
    }

    /**
     * Create and prepare list of FoldingCellViews with different bitmap parts for fold animation
     *
     * @param titleViewBitmap   bitmap from title view
     * @param contentViewBitmap bitmap from content view
     * @return list of FoldingCellViews with bitmap parts
     */
    protected ArrayList<FoldingCellView> prepareViewsForAnimation(Bitmap titleViewBitmap, Bitmap contentViewBitmap) {
        int contentViewHeight = contentViewBitmap.getHeight();
        int partHeight = titleViewBitmap.getHeight();
        int partWidth = titleViewBitmap.getWidth();
        int partsCount = contentViewHeight / partHeight;
        int restPartHeight = contentViewHeight % partHeight;

        ArrayList<FoldingCellView> partsList = new ArrayList<>();
        for (int i = 0; i < partsCount; i++) {
            Bitmap partBitmap = Bitmap.createBitmap(partWidth, partHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(partBitmap);
            Rect srcRect = new Rect(0, i * partHeight, partWidth, (i + 1) * partHeight);
            Rect destRect = new Rect(0, 0, partWidth, partHeight);
            canvas.drawBitmap(contentViewBitmap, srcRect, destRect, null);
            ImageView backView = createImageViewFromBitmap(partBitmap);
            ImageView frontView = (i == 0) ?
                    createImageViewFromBitmap(titleViewBitmap) :
                    createBackSideView((i == partsCount - 1) ? restPartHeight : partHeight);
            partsList.add(new FoldingCellView(frontView, backView, getContext()));
        }

        if (restPartHeight > 0) {
            Bitmap partBitmap = Bitmap.createBitmap(partWidth, restPartHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(partBitmap);
            Rect srcRect = new Rect(0, partsCount * partHeight, partWidth, contentViewHeight);
            Rect desRect = new Rect(0, 0, partWidth, restPartHeight);
            canvas.drawBitmap(contentViewBitmap, srcRect, desRect, null);
            partsList.add(new FoldingCellView(null, createImageViewFromBitmap(partBitmap), getContext()));
        }
        return partsList;
    }

    /**
     * Create image view for display back side of flip view
     *
     * @param height height for view
     * @return ImageView with selected height and default background color
     */
    protected ImageView createBackSideView(int height) {
        ImageView imageView = new ImageView(getContext());
        imageView.setBackgroundColor(mBackSideColor);
        imageView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        return imageView;
    }

    /**
     * Create image view for display selected bitmap
     *
     * @param bitmap bitmap to display in image view
     * @return ImageView with selected bitmap
     */
    protected ImageView createImageViewFromBitmap(Bitmap bitmap) {
        ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(bitmap);
        imageView.setLayoutParams(new LayoutParams(bitmap.getWidth(), bitmap.getHeight()));
        return imageView;
    }

    /**
     * Create bitmap from specified View with specified with
     *
     * @param view        source for bitmap
     * @param parentWidth result bitmap width
     * @return bitmap from specified view
     */
    protected Bitmap getBitmapFromView(View view, int parentWidth) {
        int specW = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY);
        int specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(specW, specH);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        return b;
    }

    /**
     * Create layout that will be a container for animation elements
     *
     * @return Configured container for animation elements (LinearLayout)
     */
    protected LinearLayout createAndPrepareFoldingContainer() {
        LinearLayout foldingContainer = new LinearLayout(getContext());
        foldingContainer.setClipToPadding(false);
        foldingContainer.setClipChildren(false);
        foldingContainer.setOrientation(LinearLayout.VERTICAL);
        foldingContainer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return foldingContainer;
    }

    /**
     * Prepare and start height expand animation for FoldingCellLayout
     *
     * @param partsCount            total count of fold animation parts
     * @param partAnimationDuration one part animate duration
     * @param bigPartHeight         height of animation part
     * @param smallPartHeight       height of last (small) animation part
     */
    protected void startExpandHeightAnimation(int partsCount, int partAnimationDuration, int bigPartHeight, int smallPartHeight) {
        ArrayList<Animation> heightAnimations = new ArrayList<>();
        for (int i = 1; i < partsCount; i++) {
            int heightDelta = i != partsCount - 1 ? bigPartHeight : smallPartHeight;
            HeightAnimation heightAnimation = new HeightAnimation(this, i * bigPartHeight, i * bigPartHeight + heightDelta);
            heightAnimation.setDuration(partAnimationDuration);
            heightAnimation.setInterpolator(new DecelerateInterpolator());
            heightAnimations.add(heightAnimation);
        }
        createAnimationChain(heightAnimations, this);
        this.startAnimation(heightAnimations.get(0));
    }

    /**
     * Prepare and start height collapse animation for FoldingCellLayout
     *
     * @param partsCount            total count of fold animation parts
     * @param partAnimationDuration one part animate duration
     * @param bigPartHeight         height of animation part
     * @param smallPartHeight       height of last (small) animation part
     */
    protected void startCollapseHeightAnimation(int partsCount, int partAnimationDuration, int bigPartHeight, int smallPartHeight) {
        ArrayList<Animation> heightAnimations = new ArrayList<>();
        for (int i = partsCount; i > 1; i--) {
            int currentHeight = (i - 1) * bigPartHeight + ((i == partsCount) ? smallPartHeight : bigPartHeight);
            int heightDelta = (i == partsCount) ? smallPartHeight : bigPartHeight;
            HeightAnimation heightAnimation = new HeightAnimation(this, currentHeight, currentHeight - heightDelta);
            heightAnimation.setDuration(partAnimationDuration);
            heightAnimation.setInterpolator(new DecelerateInterpolator());
            heightAnimations.add(heightAnimation);
        }
        createAnimationChain(heightAnimations, this);
        this.startAnimation(heightAnimations.get(0));
    }

    /**
     * Create "animation chain" for selected view from list of animations objects
     *
     * @param animationList   collection with animations
     * @param animationObject view for animations
     */
    protected void createAnimationChain(final List<Animation> animationList, final View animationObject) {
        for (int i = 0; i < animationList.size(); i++) {
            Animation animation = animationList.get(i);
            if (i + 1 < animationList.size()) {
                final int finalI = i;
                animation.setAnimationListener(new AnimationEndListener() {
                    public void onAnimationEnd(Animation animation) {
                        animationObject.startAnimation(animationList.get(finalI + 1));
                    }
                });
            }
        }
    }

    /**
     * Start fold animation
     *
     * @param foldingCellElements           ordered list with animation parts from top to bottom
     * @param foldingLayout                 prepared layout for animation parts
     * @param part90degreeAnimationDuration animation duration for 90 degree rotation
     * @param animationEndListener          animation end callback
     */
    protected void startFoldAnimation(ArrayList<FoldingCellView> foldingCellElements, ViewGroup foldingLayout,
                                      int part90degreeAnimationDuration, AnimationEndListener animationEndListener) {
        for (FoldingCellView foldingCellElement : foldingCellElements)
            foldingLayout.addView(foldingCellElement);

        Collections.reverse(foldingCellElements);

        int nextDelay = 0;
        for (int i = 0; i < foldingCellElements.size(); i++) {
            FoldingCellView cell = foldingCellElements.get(i);
            cell.setVisibility(VISIBLE);
            // not FIRST(BOTTOM) element - animate front view
            if (i != 0) {
                FoldAnimation foldAnimation = new FoldAnimation(FoldAnimation.FoldAnimationMode.UNFOLD_UP, part90degreeAnimationDuration)
                        .withStartOffset(nextDelay)
                        .withInterpolator(new DecelerateInterpolator());
                // if last(top) element - add end listener
                if (i == foldingCellElements.size() - 1) {
                    foldAnimation.setAnimationListener(animationEndListener);
                }
                cell.animateFrontView(foldAnimation);
                nextDelay = nextDelay + part90degreeAnimationDuration;
            }
            // if not last(top) element - animate whole view
            if (i != foldingCellElements.size() - 1) {
                cell.startAnimation(new FoldAnimation(FoldAnimation.FoldAnimationMode.FOLD_UP, part90degreeAnimationDuration)
                        .withStartOffset(nextDelay)
                        .withInterpolator(new DecelerateInterpolator()));
                nextDelay = nextDelay + part90degreeAnimationDuration;
            }
        }
    }

    /**
     * Start unfold animation
     *
     * @param foldingCellElements           ordered list with animation parts from top to bottom
     * @param foldingLayout                 prepared layout for animation parts
     * @param part90degreeAnimationDuration animation duration for 90 degree rotation
     * @param animationEndListener          animation end callback
     */
    protected void startUnfoldAnimation(ArrayList<FoldingCellView> foldingCellElements, ViewGroup foldingLayout,
                                        int part90degreeAnimationDuration, AnimationEndListener animationEndListener) {
        int nextDelay = 0;
        for (int i = 0; i < foldingCellElements.size(); i++) {
            FoldingCellView cell = foldingCellElements.get(i);
            cell.setVisibility(VISIBLE);
            foldingLayout.addView(cell);
            // if not first(top) element - animate whole view
            if (i != 0) {
                FoldAnimation foldAnimation = new FoldAnimation(FoldAnimation.FoldAnimationMode.UNFOLD_DOWN, part90degreeAnimationDuration)
                        .withStartOffset(nextDelay)
                        .withInterpolator(new DecelerateInterpolator());

                // if last(bottom) element - add end listener
                if (i == foldingCellElements.size() - 1) {
                    foldAnimation.setAnimationListener(animationEndListener);
                }

                nextDelay = nextDelay + part90degreeAnimationDuration;
                cell.startAnimation(foldAnimation);

            }
            // not last(bottom) element - animate front view
            if (i != foldingCellElements.size() - 1) {
                cell.animateFrontView(new FoldAnimation(FoldAnimation.FoldAnimationMode.FOLD_DOWN, part90degreeAnimationDuration)
                        .withStartOffset(nextDelay)
                        .withInterpolator(new DecelerateInterpolator()));
                nextDelay = nextDelay + part90degreeAnimationDuration;
            }
        }
    }

    /**
     * Initialize folding cell with parameters from attribute
     *
     * @param context context
     * @param attrs   attributes
     */
    protected void initializeFromAttributes(Context context, AttributeSet attrs) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FoldingCell, 0, 0);
        try {
            this.mAnimationDuration = array.getInt(R.styleable.FoldingCell_animationDuration, DEF_ANIMATION_DURATION);
            this.mBackSideColor = array.getColor(R.styleable.FoldingCell_backSideColor, DEF_BACK_SIDE_COLOR);
        } finally {
            array.recycle();
        }
    }

    /**
     * Instantly change current state of cell to Folded without any animations
     */
    protected void setStateToFolded() {
        if (this.mAnimationInProgress || !this.mUnfolded) return;
        // get basic views
        final View contentView = getChildAt(0);
        if (contentView == null) return;
        final View titleView = getChildAt(1);
        if (titleView == null) return;
        contentView.setVisibility(GONE);
        titleView.setVisibility(VISIBLE);
        FoldingCell.this.mUnfolded = false;
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.height = titleView.getHeight();
        this.setLayoutParams(layoutParams);
        this.requestLayout();
    }

    /**
     * Instantly change current state of cell to Unfolded without any animations
     */
    protected void setStateToUnfolded() {
        if (this.mAnimationInProgress || this.mUnfolded) return;
        // get basic views
        final View contentView = getChildAt(0);
        if (contentView == null) return;
        final View titleView = getChildAt(1);
        if (titleView == null) return;
        contentView.setVisibility(VISIBLE);
        titleView.setVisibility(GONE);
        FoldingCell.this.mUnfolded = true;
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.height = contentView.getHeight();
        this.setLayoutParams(layoutParams);
        this.requestLayout();
    }

}
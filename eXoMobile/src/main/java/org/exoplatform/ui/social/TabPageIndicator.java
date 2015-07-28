/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.ui.social;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import org.exoplatform.R;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
public class TabPageIndicator extends HorizontalScrollView implements PageIndicator {
  /** Title text used when no title is provided by the adapter. */
  private static final CharSequence EMPTY_TITLE = "";

  /**
   * Interface for a callback when the selected tab has been reselected.
   */
  public interface OnTabReselectedListener {
    /**
     * Callback when the selected tab has been reselected.
     * 
     * @param position Position of the current center item.
     */
    void onTabReselected(int position);
  }

  private Runnable                       mTabSelector;

  private final OnClickListener          mTabClickListener = 
		  new OnClickListener() {
             @Override
             public void onClick(View view) {
               TabView tabView = (TabView) view;
               final int oldSelected = mViewPager.getCurrentItem();
               final int newSelected = tabView.getIndex();
               mViewPager.setCurrentItem(newSelected);
               if (oldSelected == newSelected && mTabReselectedListener != null) {
                 mTabReselectedListener.onTabReselected(newSelected);
               }
             }
           };

  private final LinearLayout             mTabLayout;

  private ViewPager                      mViewPager;

  private ViewPager.OnPageChangeListener mListener;

  private int                            mMaxTabWidth;

  private int                            mSelectedTabIndex;

  private OnTabReselectedListener        mTabReselectedListener;

  public TabPageIndicator(Context context) {
    this(context, null);
  }

  public TabPageIndicator(Context context, AttributeSet attrs) {
    super(context, attrs);
    setHorizontalScrollBarEnabled(false);

    mTabLayout = new LinearLayout(getContext());
    addView(mTabLayout, new ViewGroup.LayoutParams(WRAP_CONTENT, FILL_PARENT));
  }

  public void setOnTabReselectedListener(OnTabReselectedListener listener) {
    mTabReselectedListener = listener;
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final boolean lockedExpanded = widthMode == MeasureSpec.EXACTLY;
    setFillViewport(lockedExpanded);

    final int childCount = mTabLayout.getChildCount();
    if (childCount > 1 && (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST)) {
      if (childCount > 2) {
        mMaxTabWidth = (int) (MeasureSpec.getSize(widthMeasureSpec) * 0.5f);
      } else {
        mMaxTabWidth = MeasureSpec.getSize(widthMeasureSpec) / 2;
      }
    } else {
      mMaxTabWidth = -1;
    }

    final int oldWidth = getMeasuredWidth();
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    final int newWidth = getMeasuredWidth();

    if (lockedExpanded && oldWidth != newWidth) {
      // Recenter the tab display if we're at a new (scrollable) size.
      setCurrentItem(mSelectedTabIndex);
    }
  }

  private void animateToTab(final int position) {
    final View tabView = mTabLayout.getChildAt(position);
    if (mTabSelector != null) {
      removeCallbacks(mTabSelector);
    }
    mTabSelector = new Runnable() {
      @Override
      public void run() {
        final int scrollPos = tabView.getLeft() - (getWidth() - tabView.getWidth()) / 2;
        smoothScrollTo(scrollPos, 0);
        mTabSelector = null;
      }
    };
    post(mTabSelector);
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (mTabSelector != null) {
      // Re-post the selector we saved
      post(mTabSelector);
    }
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (mTabSelector != null) {
      removeCallbacks(mTabSelector);
    }
  }

  private void addTab(CharSequence text, int index) {
    final TabView tabView = new TabView(getContext());
    tabView.mIndex = index;
    tabView.setFocusable(true);
    tabView.setOnClickListener(mTabClickListener);
    tabView.setText(text);
    mTabLayout.addView(tabView, new LinearLayout.LayoutParams(WRAP_CONTENT, FILL_PARENT, 1));
  }

  @Override
  public void onPageScrollStateChanged(int arg0) {
    if (mListener != null) {
      mListener.onPageScrollStateChanged(arg0);
    }
  }

  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {
    if (mListener != null) {
      mListener.onPageScrolled(arg0, arg1, arg2);
    }
  }

  @Override
  public void onPageSelected(int arg0) {
    setCurrentItem(arg0);
    if (mListener != null) {
      mListener.onPageSelected(arg0);
    }
  }

  @Override
  public void setViewPager(ViewPager view) {
    if (view.equals(mViewPager)) {
      return;
    }
    if (mViewPager != null) {
      mViewPager.setOnPageChangeListener(null);
    }
    final PagerAdapter adapter = view.getAdapter();
    if (adapter == null) {
      throw new IllegalStateException("ViewPager does not have adapter instance.");
    }
    mViewPager = view;
    view.setOnPageChangeListener(this);
    notifyDataSetChanged();
  }

  @Override
  public void notifyDataSetChanged() {
    mTabLayout.removeAllViews();
    PagerAdapter adapter = mViewPager.getAdapter();
    final int count = adapter.getCount();
    for (int i = 0; i < count; i++) {
      CharSequence title = adapter.getPageTitle(i);
      if (title == null) {
        title = EMPTY_TITLE;
      }
      addTab(title, i);
    }
    if (mSelectedTabIndex > count) {
      mSelectedTabIndex = count - 1;
    }
    setCurrentItem(mSelectedTabIndex);
    requestLayout();
  }

  @Override
  public void setViewPager(ViewPager view, int initialPosition) {
    setViewPager(view);
    setCurrentItem(initialPosition);
  }

  @Override
  public void setCurrentItem(int item) {
    if (mViewPager == null) {
      throw new IllegalStateException("ViewPager has not been bound.");
    }
    mSelectedTabIndex = item;
    mViewPager.setCurrentItem(item);

    final int tabCount = mTabLayout.getChildCount();
    for (int i = 0; i < tabCount; i++) {
      TabView child = (TabView) mTabLayout.getChildAt(i);
      final boolean isSelected = (i == item);
      child.setSelected(isSelected);
      /*
       * Set text color in case of selected
       */
      if (isSelected) {
        child.setTextColor(Color.rgb(65, 65, 65));
        animateToTab(item);
      } else
        child.setTextColor(Color.rgb(120, 120, 120));
    }
  }

  @Override
  public void setOnPageChangeListener(OnPageChangeListener listener) {
    mListener = listener;
  }

  private class TabView extends TextView {
    private int mIndex;

    public TabView(Context context) {
      super(context, null, R.attr.vpiTabPageIndicatorStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);

      // Re-measure if we went beyond our maximum size.
      if (mMaxTabWidth > 0 && getMeasuredWidth() > mMaxTabWidth) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(mMaxTabWidth, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
      }
    }

    public int getIndex() {
      return mIndex;
    }
  }
}

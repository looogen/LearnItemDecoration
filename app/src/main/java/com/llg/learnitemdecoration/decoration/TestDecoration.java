package com.llg.learnitemdecoration.decoration;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.llg.learnitemdecoration.adapter.RecyclerViewAdapter;

/**
 * create by lilugen on 2018/7/31
 */
public class TestDecoration extends RecyclerView.ItemDecoration {
    private RecyclerViewAdapter mAdapter;
    private final SparseArray<Rect> mHeaderRects = new SparseArray<>();

    public TestDecoration(RecyclerViewAdapter mAdapter) {
        this.mAdapter = mAdapter;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int position = parent.getChildAdapterPosition(view);
        int headerHeight = 0;
        //在使用adapterPosition时最好的加上这个判断
        if (position != RecyclerView.NO_POSITION && hasHeader(position)) {
            //获取到ItemDecoration所需要的高度
            View header = getHeader(parent, position);
            headerHeight = header.getHeight();
        }
        outRect.set(0, headerHeight, 0, 0);

    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        //mHeaderRects为存放屏幕上显示的header的点击区域，每次重新绘制头部的时候清空数据
        mHeaderRects.clear();
        final int count = parent.getChildCount();
        //遍历屏幕上加载的item
        for (int i = 0; i < count; i++) {
            final View child = parent.getChildAt(i);
            //获取该item在列表数据中的位置
            final int position = parent.getChildAdapterPosition(child);
            //只有在最上面一个item或者有header的item才绘制header
            if (position != RecyclerView.NO_POSITION && (i == 0 || hasHeader(position))) {
                View header = getHeader(parent, position);
                c.save();
                //获取绘制header的起始位置(left,top)
                final int left = child.getLeft();
                final int top = getHeaderTop(parent, child, header, position, i);
                //将画布移动到绘制的位置
                c.translate(left, top);
                //绘制header
                header.draw(c);
                c.restore();
                //保存绘制的header的区域
                mHeaderRects.put(position, new Rect(left, top, left + header.getWidth(), top + header.getHeight()));
            }
        }
    }

    public int findHeaderPositionUnder(int x, int y) {
        for (int i = 0; i < mHeaderRects.size(); i++) {
            Rect rect = mHeaderRects.get(mHeaderRects.keyAt(i));
            if (rect.contains(x, y)) {
                return mHeaderRects.keyAt(i);
            }
        }
        return -1;
    }

    public boolean findHeaderClickView(View view, int x, int y) {
        if (view == null) return false;
        for (int i = 0; i < mHeaderRects.size(); i++) {
            Rect rect = mHeaderRects.get(mHeaderRects.keyAt(i));
            if (rect.contains(x, y)) {
                Rect vRect = new Rect();
                // 需要响应点击事件的区域在屏幕上的坐标
                vRect.set(rect.left + view.getLeft(), rect.top + view.getTop(), rect.left + view.getLeft() + view.getWidth(), rect.top + view.getTop() + view.getHeight());
                return vRect.contains(x, y);
            }
        }
        return false;
    }

    /**
     * 判断是否有header
     *
     * @param position
     * @return
     */
    private boolean hasHeader(int position) {
        return mAdapter.hasHeader(position);
    }

    /**
     * 获得自定义的Header
     *
     * @param parent
     * @param position
     * @return
     */
    public View getHeader(RecyclerView parent, int position) {
        //创建HeaderViewHolder
        RecyclerViewAdapter.HeaderHolder holder = mAdapter.onCreateHeaderViewHolder(parent);
        View header = holder.itemView;
        //绑定数据
        mAdapter.onBindHeaderViewHolder(holder, position);
        //测量View并且layout
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
        //根据父View的MeasureSpec和子view自身的LayoutParams以及padding来获取子View的MeasureSpec
        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                parent.getPaddingLeft() + parent.getPaddingRight(), header.getLayoutParams().width);
        int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                parent.getPaddingTop() + parent.getPaddingBottom(), header.getLayoutParams().height);
        //进行测量
        header.measure(childWidthSpec, childHeightSpec);
        //根据测量后的宽高放置位置
        header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
        //将创建好的头部view保存在数组中，避免每次重复创建
        return header;
    }

    /**
     * 计算距离顶部的高度
     *
     * @param parent
     * @param child
     * @param header
     * @param adapterPos
     * @param layoutPos
     * @return
     */
    private int getHeaderTop(RecyclerView parent, View child, View header, int adapterPos, int layoutPos) {
        int headerHeight = header.getHeight();
        int top = ((int) child.getY()) - headerHeight;
        //在绘制最顶部的header的时候，需要考虑处理两个分组的header交换时候的情况
        if (layoutPos == 0) {
            final int count = parent.getChildCount();
            final int currentId = mAdapter.getHeaderId(adapterPos);
            //从第二个屏幕上线上的第二个item开始遍历
            for (int i = 1; i < count; i++) {
                int nextpos = parent.getChildAdapterPosition(parent.getChildAt(i));
                if (nextpos != RecyclerView.NO_POSITION) {
                    int nextId = mAdapter.getHeaderId(nextpos);
                    //找到下一个不同组的view
                    if (currentId != nextId) {
                        final View next = parent.getChildAt(i);
                        //当不同组的第一个view距离顶部的位置减去两组header的高度，得到offset
                        final int offset = ((int) next.getY()) - (headerHeight + getHeader(parent, nextpos).getHeight());
                        //offset小于0即为两组开始交换，第一个header被挤出界面的距离
                        if (offset < 0) {
                            return offset;
                        } else {
                            break;
                        }
                    }
                }
            }
            top = Math.max(0, top);
        }
        return top;
    }
}
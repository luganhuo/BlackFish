package top.omooo.blackfish.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.GridLayoutHelper;
import com.alibaba.android.vlayout.layout.SingleLayoutHelper;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import top.omooo.blackfish.MallPagerActivity.ClassifyGoodsActivity;
import top.omooo.blackfish.MallPagerActivity.SearchActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.GeneralVLayoutAdapter;
import top.omooo.blackfish.adapter.GridOnlyImageAdapter;
import top.omooo.blackfish.adapter.MallHotClassifyGridAdapter;
import top.omooo.blackfish.bean.BannerInfo;
import top.omooo.blackfish.bean.GridInfo;
import top.omooo.blackfish.bean.MallGoodsInfo;
import top.omooo.blackfish.bean.MallGoodsItemInfo;
import top.omooo.blackfish.bean.MallHotClassifyGridInfo;
import top.omooo.blackfish.bean.MallPagerInfo;
import top.omooo.blackfish.bean.UrlInfoBean;
import top.omooo.blackfish.listener.OnNetResultListener;
import top.omooo.blackfish.utils.AnalysisJsonUtil;
import top.omooo.blackfish.utils.OkHttpUtil;
import top.omooo.blackfish.view.CustomToast;
import top.omooo.blackfish.view.GridViewForScroll;
import top.omooo.blackfish.view.RecycleViewBanner;

/**
 * Created by Omooo on 2018/2/25.
 */

public class MallFragment extends BaseFragment {

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private Context mContext;

    private Toolbar mToolbar;
    private ImageView mImageMenu, mImageMsg;
    private RelativeLayout mHeaderLayout;

    private RecycleViewBanner mBanner;
    private List<BannerInfo> mBannerInfos;

    final List<DelegateAdapter.Adapter> adapters = new LinkedList<>();
    private static final String TAG = "MallFragment";
    private VirtualLayoutManager layoutManager;
    private RecyclerView.RecycledViewPool viewPool;
    private DelegateAdapter delegateAdapter;

    private SimpleDraweeView mImageGridItem;
    private TextView mTextGridItem;

    private AnalysisJsonUtil mJsonUtil = new AnalysisJsonUtil();
    private List<MallPagerInfo> mMallPagerInfos;
    private List<GridInfo> mGridInfos;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0x01:
                    addItemViews(mMallPagerInfos.get(0));
                    break;
                default:break;
            }
            return false;
        }
    });

    public static MallFragment newInstance() {
        return new MallFragment();
    }


    @Override
    public int getLayoutId() {
        return R.layout.fragment_mall_layout;
    }

    @Override
    public void initViews() {

        getActivity().getWindow().setStatusBarColor(Color.parseColor("#00000000"));
        mToolbar = findView(R.id.toolbar_mall);
        mToolbar.getBackground().setAlpha(0);

        mContext = getActivity();
        mRecyclerView = findView(R.id.rv_fragment_mall_container);
        mRefreshLayout = findView(R.id.swipe_container);

        mImageMenu = findView(R.id.iv_mall_header_menu);
        mImageMsg = findView(R.id.iv_mall_header_msg);
        mHeaderLayout = findView(R.id.rl_mall_header_layout);

        layoutManager = new VirtualLayoutManager(mContext);
        mRecyclerView.setLayoutManager(layoutManager);

        viewPool = new RecyclerView.RecycledViewPool();
        mRecyclerView.setRecycledViewPool(viewPool);
        viewPool.setMaxRecycledViews(0, 20);

        delegateAdapter = new DelegateAdapter(layoutManager, false);
        mRecyclerView.setAdapter(delegateAdapter);

    }

    private void addItemViews(final MallPagerInfo mallPagerInfo) {

        //轮播图
        SingleLayoutHelper bannerHelper = new SingleLayoutHelper();
        GeneralVLayoutAdapter bannerAdapter = new GeneralVLayoutAdapter(mContext, bannerHelper, 1) {

            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_banner_layout, parent,false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                mBanner = holder.itemView.findViewById(R.id.rvb_mall_header);
                mBannerInfos = mallPagerInfo.getBannerInfos();
                mBanner.setRvBannerData(mBannerInfos);
                mBanner.setOnSwitchRvBannerListener(new RecycleViewBanner.OnSwitchRvBannerListener() {
                    @Override
                    public void switchBanner(int position, SimpleDraweeView simpleDraweeView) {
                        simpleDraweeView.setImageURI(mBannerInfos.get(position).getUrl());
                    }
                });
                mBanner.setOnBannerClickListener(new RecycleViewBanner.OnRvBannerClickListener() {
                    @Override
                    public void onClick(int position) {
                        Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        adapters.add(bannerAdapter);

        GridLayoutHelper gridLayoutHelper = new GridLayoutHelper(5);
        GeneralVLayoutAdapter gridAdapter = new GeneralVLayoutAdapter(mContext, gridLayoutHelper, 10){
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_two_line_grid, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                mImageGridItem = holder.itemView.findViewById(R.id.iv_mall_grid_item);
                mTextGridItem = holder.itemView.findViewById(R.id.tv_mall_grid_item);
                mGridInfos = mallPagerInfo.getClassifyInfos();
                mImageGridItem.setImageURI(mGridInfos.get(position).getImageUrl());
                mTextGridItem.setText(mGridInfos.get(position).getTitle());

            }
        };
        adapters.add(gridAdapter);

        SingleLayoutHelper fourGoodsHelper = new SingleLayoutHelper();
        GeneralVLayoutAdapter fourGoodsAdapter = new GeneralVLayoutAdapter(mContext, fourGoodsHelper, 1) {

            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_four_goods_layout, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                SimpleDraweeView headerImage = holder.itemView.findViewById(R.id.iv_four_header_image);
                GridViewForScroll gridFourGoods = holder.itemView.findViewById(R.id.gv_four_goods);
                gridFourGoods.setAdapter(new GridOnlyImageAdapter(mContext,mallPagerInfo.getGridGoodsInfos()));
                headerImage.setImageURI(mallPagerInfo.getSingleImageUrl());
            }
        };
        adapters.add(fourGoodsAdapter);

        GridLayoutHelper gridHotClassifyHelper = new GridLayoutHelper(1);
        GeneralVLayoutAdapter hotClassifyAdapter = new GeneralVLayoutAdapter(mContext, gridHotClassifyHelper, mallPagerInfo.getMallGoodsInfos().size()){
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_hot_classify_grid_layout, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);

                List<MallHotClassifyGridInfo> mallHotClassifyGridInfos = new ArrayList<>();

                SimpleDraweeView headerImage = holder.itemView.findViewById(R.id.iv_hot_classify_header_image);
                GridViewForScroll gridGoods = holder.itemView.findViewById(R.id.gv_hot_classify);
                List<MallGoodsInfo> mallGoodsInfos = mallPagerInfo.getMallGoodsInfos();
                headerImage.setImageURI(mallGoodsInfos.get(position).getHeaderImageUrl());

                int itemSize = mallGoodsInfos.get(position).getMallGoodsItemInfos().size();
                Log.i(TAG, "onBindViewHolder: " + itemSize);
                for (int i = 0; i < itemSize; i++) {
                    MallGoodsItemInfo mallGoodsItemInfo = mallGoodsInfos.get(position).getMallGoodsItemInfos().get(i);
                    String goodsImage = mallGoodsItemInfo.getImageUrl();
                    String goodsDesc = mallGoodsItemInfo.getDesc();
                    String goodsPeriods = "¥" + mallGoodsItemInfo.getSinglePrice() + " x " + mallGoodsItemInfo.getPeriods() + "期";
                    String goodsPrice = "¥" + mallGoodsItemInfo.getPrice();
                    mallHotClassifyGridInfos.add(new MallHotClassifyGridInfo(goodsImage, goodsDesc, goodsPeriods, goodsPrice));
                }
                gridGoods.setAdapter(new MallHotClassifyGridAdapter(mContext, mallHotClassifyGridInfos));

            }
        };
        adapters.add(hotClassifyAdapter);

        delegateAdapter.setAdapters(adapters);
    }

    @Override
    public void initListener() {
        mImageMenu.setOnClickListener(this);
        mImageMsg.setOnClickListener(this);
        mHeaderLayout.setOnClickListener(this);
    }

    @Override
    public void initData() {
        mJsonUtil = new AnalysisJsonUtil();
        mMallPagerInfos = new ArrayList<>();
        OkHttpUtil.getInstance().startGet(UrlInfoBean.mallGoodsUrl, new OnNetResultListener() {
            @Override
            public void onSuccessListener(String result) {
                mMallPagerInfos = mJsonUtil.getDataFromJson(result, 3);

                Message message = mHandler.obtainMessage(0x01, mMallPagerInfos);
                mHandler.sendMessage(message);

            }

            @Override
            public void onFailureListener(String result) {
                Log.i(TAG, "onFailureListener: "+result);
            }
        });
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_mall_header_menu:
                startActivity(new Intent(mContext, ClassifyGoodsActivity.class));
                break;
            case R.id.iv_mall_header_msg:
                CustomToast.show(mContext, "消息中心");
                break;
            case R.id.rl_mall_header_layout:
                startActivity(new Intent(mContext, SearchActivity.class));
                break;
            default:break;
        }
    }

}

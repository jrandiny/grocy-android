package xyz.zedler.patrick.grocy.fragment;

import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.MissingBatchItemAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.MissingBatchItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MissingBatchItemsFragment extends Fragment implements MissingBatchItemAdapter.MissingBatchItemAdapterListener {

    private final static String TAG = Constants.UI.MISSING_BATCH_ITEMS;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private MissingBatchItemAdapter missingBatchItemAdapter;
    private BroadcastReceiver broadcastReceiver;
    private ActionButton actionButtonFlash;

    private ArrayList<MissingBatchItem> missingBatchItems;
    private String itemsToDisplay = Constants.STOCK.FILTER.ALL;
    private String search = "";
    private String sortMode;
    private boolean sortAscending;

    private RecyclerView recyclerView;
    private NestedScrollView scrollView;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_missing_batch_items, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        scrollView = activity.findViewById(R.id.scroll_missing_batch_items);
        recyclerView = activity.findViewById(R.id.recycler_missing_batch_items);

        if(getArguments() == null ||
                getArguments().getParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS) == null
        ) {
            setError(true, false, false);
            activity.findViewById(R.id.button_error_retry).setVisibility(View.GONE);
        } else {
            missingBatchItems = getArguments().getParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS);
        }

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new MissingBatchItemAdapter(missingBatchItems, this));

        // UPDATE UI

        activity.updateUI(Constants.UI.MISSING_BATCH_ITEMS, TAG);
    }

    public void createdProduct(Bundle bundle) {
        if(bundle != null && bundle.getString(Constants.ARGUMENT.PRODUCT_NAME) != null) {
            // TODO
        }
    }

    public int getMissingProductsSize() { // TODO
        return missingBatchItems.size();
    }

    public void exitWarning() {
        // TODO
    }

    private void setError(boolean isError, boolean isOffline, boolean animated) {
        LinearLayout linearLayoutError = activity.findViewById(R.id.linear_error);
        ImageView imageViewError = activity.findViewById(R.id.image_error);
        TextView textViewTitle = activity.findViewById(R.id.text_error_title);
        TextView textViewSubtitle = activity.findViewById(R.id.text_error_subtitle);

        if(isError) {
            imageViewError.setImageResource(
                    isOffline
                            ? R.drawable.illustration_broccoli
                            : R.drawable.illustration_popsicle
            );
            textViewTitle.setText(isOffline ? R.string.error_offline : R.string.error_unknown);
            textViewSubtitle.setText(
                    isOffline
                            ? R.string.error_offline_subtitle
                            : R.string.error_unknown_subtitle
            );
        }

        if(animated) {
            View viewOut = isError ? scrollView : linearLayoutError;
            View viewIn = isError ? linearLayoutError : scrollView;
            if(viewOut.getVisibility() == View.VISIBLE && viewIn.getVisibility() == View.GONE) {
                viewOut.animate().alpha(0).setDuration(150).withEndAction(() -> {
                    viewIn.setAlpha(0);
                    viewOut.setVisibility(View.GONE);
                    viewIn.setVisibility(View.VISIBLE);
                    viewIn.animate().alpha(1).setDuration(150).start();
                }).start();
            }
        } else {
            scrollView.setVisibility(isError ? View.GONE : View.VISIBLE);
            linearLayoutError.setVisibility(isError ? View.VISIBLE : View.GONE);
        }
    }

    /*private StockItem getStockItem(int productId) {
        for(StockItem stockItem : displayedItems) {
            if(stockItem.getProduct().getId() == productId) {
                return stockItem;
            }
        } return null;
    }*/

    private void showErrorMessage() {
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    @Override
    public void onItemRowClicked(int position) {
        MissingBatchItem batchItem = missingBatchItems.get(position);

        String defaultBestBeforeDays = String.valueOf(batchItem.getDefaultBestBeforeDays());
        String defaultLocationId = String.valueOf(batchItem.getDefaultLocationId());

        CreateProduct createProduct = new CreateProduct(
                batchItem.getProductName(),
                batchItem.getBarcodes(),
                batchItem.getDefaultStoreId(),
                defaultBestBeforeDays,
                defaultLocationId
        );
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE_BATCH);
        bundle.putParcelable(Constants.ARGUMENT.CREATE_PRODUCT_OBJECT, createProduct);

        activity.replaceFragment(Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE, bundle, true);
    }

    private void refreshAdapter(MissingBatchItemAdapter adapter) {
        missingBatchItemAdapter = adapter;
        recyclerView.animate().alpha(0).setDuration(150).withEndAction(() -> {
            recyclerView.setAdapter(adapter);
            recyclerView.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    public void setUpBottomMenu() {}

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}

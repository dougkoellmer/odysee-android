package com.odysee.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.R;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.LbryFile;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import lombok.Getter;
import lombok.Setter;

public class ClaimListAdapter extends RecyclerView.Adapter<ClaimListAdapter.ViewHolder> {
    private static final int VIEW_TYPE_STREAM = 1;
    private static final int VIEW_TYPE_CHANNEL = 2;
    private static final int VIEW_TYPE_FEATURED = 3; // featured search result
    @Getter
    @Setter
    private int contextGroupId;

    private final Map<String, Claim> quickClaimIdMap;
    private final Map<String, Claim> quickClaimUrlMap;
    private final Map<String, Boolean> notFoundClaimIdMap;
    private final Map<String, Boolean> notFoundClaimUrlMap;

    @Setter
    private boolean hideFee;
    @Setter
    private boolean canEnterSelectionMode;
    private final Context context;
    private List<Claim> items;
    private final List<Claim> selectedItems;
    @Setter
    private ClaimListItemListener listener;
    @Getter
    @Setter
    private boolean inSelectionMode;
    @Setter
    private SelectionModeListener selectionModeListener;
    @Getter
    @Setter
    private int position;

    public ClaimListAdapter(List<Claim> items, Context context) {
        this.context = context;
        this.items = new ArrayList<>();
        for (Claim item : items) {
            if (item != null) {
                this.items.add(item);
            }
        }

        this.selectedItems = new ArrayList<>();
        quickClaimIdMap = new HashMap<>();
        quickClaimUrlMap = new HashMap<>();
        notFoundClaimIdMap = new HashMap<>();
        notFoundClaimUrlMap = new HashMap<>();
    }

    public List<Claim> getSelectedItems() {
        return this.selectedItems;
    }
    public int getSelectedCount() {
        return selectedItems != null ? selectedItems.size() : 0;
    }
    public void clearSelectedItems() {
        this.selectedItems.clear();
    }
    public boolean isClaimSelected(Claim claim) {
        return selectedItems.contains(claim);
    }

    public Claim getFeaturedItem() {
        for (Claim claim : items) {
            if (claim.isFeatured()) {
                return claim;
            }
        }
        return null;
    }

    public void removeFeaturedItem() {
        int featuredIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isFeatured()) {
                featuredIndex = i;
                break;
            }
        }
        if (featuredIndex > -1) {
            items.remove(featuredIndex);
        }
    }

    public List<Claim> getItems() {
        return new ArrayList<>(this.items);
    }

    public void updateSigningChannelForClaim(Claim resolvedClaim) {
        for (Claim claim : items) {
            if (claim.getClaimId().equalsIgnoreCase(resolvedClaim.getClaimId())) {
                claim.setSigningChannel(resolvedClaim.getSigningChannel());
            }
        }
    }

    public void filterBlockedChannels(List<LbryUri> blockedChannels) {
        if (blockedChannels.size() == 0) {
            return;
        }
        List<Claim> claimsToRemove = new ArrayList<>();
        List<String> blockedChannelClaimIds = new ArrayList<>();
        for (LbryUri uri : blockedChannels) {
            blockedChannelClaimIds.add(uri.getClaimId());
        }
        for (Claim claim : items) {
            if (claim.getSigningChannel() != null && blockedChannelClaimIds.contains(claim.getSigningChannel().getClaimId())) {
                claimsToRemove.add(claim);
            }
        }
        items.removeAll(claimsToRemove);
        notifyDataSetChanged();
    }

    public void clearItems() {
        clearSelectedItems();
        this.items.clear();
        quickClaimIdMap.clear();
        quickClaimUrlMap.clear();
        notFoundClaimIdMap.clear();
        notFoundClaimUrlMap.clear();
        notifyDataSetChanged();
    }

    public Claim getLastItem() {
        return items.size() > 0 ? items.get(items.size() - 1) : null;
    }

    public void addFeaturedItem(Claim claim) {
        items.add(0, claim);
        notifyItemInserted(0);
    }

    public void addItems(List<Claim> claims) {
        final int initialCount = items.size();

        for (Claim claim : claims) {
            if (claim != null) {
                boolean c = items.stream().anyMatch(p -> p.getClaimId() != null && p.getClaimId().equalsIgnoreCase(claim.getClaimId()));

                if (!c) {
                    items.add(claim);
                }
            }
        }

        notifyItemRangeInserted(initialCount, items.size() - initialCount);

        notFoundClaimUrlMap.clear();
        notFoundClaimIdMap.clear();
    }
    public void setItems(List<Claim> claims) {
        if (items.size() > 0)
            notifyItemRangeRemoved(0, items.size());

        items = new ArrayList<>();
        for (Claim claim : claims) {
            if (claim != null) {
                items.add(claim);
            }
        }
        notifyItemRangeInserted(0, items.size());
    }

    public void setItem(String claimId, Claim claim) {
        if (claim != null) {
            Claim r = this.items.stream().filter(o -> o.getClaimId() != null && o.getClaimId().equalsIgnoreCase(claimId)).findFirst().orElse(null);

            if (r != null) {
                int position = this.items.indexOf(r);
                items.set(this.items.indexOf(r), claim);
                notifyItemChanged(position, claim);
            }
        }
    }
    public void removeItems(List<Claim> claims) {
        items.removeAll(claims);
        notifyDataSetChanged();
    }

    public void removeItem(Claim claim) {
        int position = items.indexOf(claim);
        items.remove(claim);
        selectedItems.remove(claim);
        notifyItemRemoved(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        @Getter
        @Setter
        private int contextGroupId;

        protected final View feeContainer;
        protected final TextView feeView;
        protected final ImageView thumbnailView;
        protected final View noThumbnailView;
        protected final TextView alphaView;
        protected final TextView vanityUrlView;
        protected final TextView durationView;
        protected final TextView titleView;
        protected final TextView publisherView;
        protected final TextView publishTimeView;
        protected final TextView pendingTextView;
        protected final View repostInfoView;
        protected final TextView repostChannelView;
        protected final View selectedOverlayView;
        protected final TextView fileSizeView;
        protected final ProgressBar downloadProgressView;
        protected final TextView deviceView;
        protected final ImageButton optionsMenuView;

        protected final View loadingImagePlaceholder;
        protected final View loadingTextPlaceholder1;
        protected final View loadingTextPlaceholder2;
        public ViewHolder(View v) {
            super(v);
            feeContainer = v.findViewById(R.id.claim_fee_container);
            feeView = v.findViewById(R.id.claim_fee);
            alphaView = v.findViewById(R.id.claim_thumbnail_alpha);
            noThumbnailView = v.findViewById(R.id.claim_no_thumbnail);
            thumbnailView = v.findViewById(R.id.claim_thumbnail);
            vanityUrlView = v.findViewById(R.id.claim_vanity_url);
            durationView = v.findViewById(R.id.claim_duration);
            titleView = v.findViewById(R.id.claim_title);
            publisherView = v.findViewById(R.id.claim_publisher);
            publishTimeView = v.findViewById(R.id.claim_publish_time);
            pendingTextView = v.findViewById(R.id.claim_pending_text);
            repostInfoView = v.findViewById(R.id.claim_repost_info);
            repostChannelView = v.findViewById(R.id.claim_repost_channel);
            selectedOverlayView = v.findViewById(R.id.claim_selected_overlay);
            fileSizeView = v.findViewById(R.id.claim_file_size);
            downloadProgressView = v.findViewById(R.id.claim_download_progress);
            deviceView = v.findViewById(R.id.claim_view_device);
            optionsMenuView = v.findViewById(R.id.claim_overflow_menu_icon);

            loadingImagePlaceholder = v.findViewById(R.id.claim_thumbnail_placeholder);
            loadingTextPlaceholder1 = v.findViewById(R.id.claim_text_loading_placeholder_1);
            loadingTextPlaceholder2 = v.findViewById(R.id.claim_text_loading_placeholder_2);

            v.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            contextMenu.add(contextGroupId, R.id.action_block, Menu.NONE, R.string.block_channel);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position).isFeatured()) {
            return VIEW_TYPE_FEATURED;
        }

        Claim claim = items.get(position);
        String valueType = items.get(position).getValueType();
        Claim actualClaim = Claim.TYPE_REPOST.equalsIgnoreCase(valueType) ? claim.getRepostedClaim() : claim;

        return Claim.TYPE_CHANNEL.equalsIgnoreCase(actualClaim.getValueType()) ? VIEW_TYPE_CHANNEL : VIEW_TYPE_STREAM;
    }

    public void updateFileForClaimByIdOrUrl(LbryFile file, String claimId, String url) {
        updateFileForClaimByIdOrUrl(file, claimId, url,  false);
    }
    public void updateFileForClaimByIdOrUrl(LbryFile file, String claimId, String url, boolean skipNotFound) {
        if (!skipNotFound) {
            if (notFoundClaimIdMap.containsKey(claimId) && notFoundClaimUrlMap.containsKey(url)) {
                return;
            }
        }
        if (quickClaimIdMap.containsKey(claimId)) {
            quickClaimIdMap.get(claimId).setFile(file);
            notifyDataSetChanged();
            return;
        }
        if (quickClaimUrlMap.containsKey(claimId)) {
            quickClaimUrlMap.get(claimId).setFile(file);
            notifyDataSetChanged();
            return;
        }

        boolean claimFound = false;
        for (int i = 0; i < items.size(); i++) {
            Claim claim = items.get(i);
            if (claimId.equalsIgnoreCase(claim.getClaimId()) || url.equalsIgnoreCase(claim.getPermanentUrl())) {
                quickClaimIdMap.put(claimId, claim);
                quickClaimUrlMap.put(url, claim);
                claim.setFile(file);
                notifyDataSetChanged();
                claimFound = true;
                break;
            }
        }

        if (!claimFound) {
            notFoundClaimIdMap.put(claimId, true);
            notFoundClaimUrlMap.put(url, true);
        }
    }
    public void clearFileForClaimOrUrl(String outpoint, String url) {
        clearFileForClaimOrUrl(outpoint, url, false);
        notifyDataSetChanged();
    }


    public void clearFileForClaimOrUrl(String outpoint, String url, boolean remove) {
        int claimIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            Claim claim = items.get(i);
            if (outpoint.equalsIgnoreCase(claim.getOutpoint()) || url.equalsIgnoreCase(claim.getPermanentUrl())) {
                claimIndex = i;
                claim.setFile(null);
                break;
            }
        }
        if (remove && claimIndex > -1) {
            Claim removed = items.remove(claimIndex);
            selectedItems.remove(removed);
        }

        notifyDataSetChanged();
    }

    @Override
    public ClaimListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int viewResourceId = -1;
        switch (viewType) {
            case VIEW_TYPE_FEATURED: viewResourceId = R.layout.list_item_featured_search_result; break;
            case VIEW_TYPE_CHANNEL: viewResourceId = R.layout.list_item_channel; break;
            case VIEW_TYPE_STREAM: default: viewResourceId = R.layout.list_item_stream; break;
        }

        View v = LayoutInflater.from(context).inflate(viewResourceId, parent, false);
        return new ClaimListAdapter.ViewHolder(v);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        if (holder.optionsMenuView != null) {
            holder.optionsMenuView.setOnClickListener(null);
        }
        super.onViewRecycled(holder);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(ClaimListAdapter.ViewHolder vh, int position) {
        int type = getItemViewType(position);
        /*int paddingTop = 0; //position == 0 ? 16 : 8;
        int paddingBottom = 0; //position == getItemCount() - 1 ? 16 : 8;
        int paddingTopScaled = Helper.getScaledValue(paddingTop, scale);
        int paddingBottomScaled = Helper.getScaledValue(paddingBottom, scale);
        vh.itemView.setPadding(vh.itemView.getPaddingStart(), paddingTopScaled, vh.itemView.getPaddingEnd(), paddingBottomScaled);*/

        Claim original = items.get(position);
        boolean isRepost = Claim.TYPE_REPOST.equalsIgnoreCase(original.getValueType());
        final Claim item = Claim.TYPE_REPOST.equalsIgnoreCase(original.getValueType()) ?
                (original.getRepostedClaim() != null ? original.getRepostedClaim() : original): original;
        Claim.GenericMetadata metadata = item.getValue();
        Claim signingChannel = item.getSigningChannel();
        Claim.StreamMetadata streamMetadata = null;
        if (metadata instanceof Claim.StreamMetadata) {
            streamMetadata = (Claim.StreamMetadata) metadata;
        }

        String thumbnailUrl = item.getThumbnailUrl(vh.thumbnailView.getLayoutParams().width, vh.thumbnailView.getLayoutParams().height, 85);
        long publishTime = (streamMetadata != null && streamMetadata.getReleaseTime() > 0) ? streamMetadata.getReleaseTime() * 1000 : item.getTimestamp() * 1000;
        int bgColor = Helper.generateRandomColorForValue(item.getClaimId());
        if (bgColor == 0) {
            bgColor = Helper.generateRandomColorForValue(item.getName());
        }

        boolean isPending = item.getConfirmations() == 0;
        boolean isSelected = isClaimSelected(original);
        vh.itemView.setSelected(isSelected);
        vh.setContextGroupId(contextGroupId);
        vh.selectedOverlayView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPending) {
                    Snackbar snackbar = Snackbar.make(vh.itemView, R.string.item_pending_blockchain, Snackbar.LENGTH_LONG);
                    TextView snackbarText = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    snackbarText.setMaxLines(5);
                    snackbar.show();
                    return;
                }

                if (inSelectionMode) {
                    toggleSelectedClaim(original);
                } else {
                    if (listener != null) {
                        listener.onClaimClicked(item);
                    }
                }
            }
        });
        vh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!canEnterSelectionMode) {
                    return false;
                }

                if (isPending) {
                    Snackbar snackbar = Snackbar.make(vh.itemView, R.string.item_pending_blockchain, Snackbar.LENGTH_LONG);
                    TextView snackbarText = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    snackbarText.setMaxLines(5);
                    snackbar.show();
                    return false;
                }

                if (!inSelectionMode) {
                    inSelectionMode = true;
                    if (selectionModeListener != null) {
                        selectionModeListener.onEnterSelectionMode();
                    }
                }
                toggleSelectedClaim(original);
                return true;
            }
        });

        vh.publisherView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null && signingChannel != null) {
                    listener.onClaimClicked(signingChannel);
                }
            }
        });

        vh.publishTimeView.setVisibility(!isPending ? View.VISIBLE : View.GONE);
        vh.pendingTextView.setVisibility(isPending && !item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        vh.repostInfoView.setVisibility(isRepost && type != VIEW_TYPE_FEATURED ? View.VISIBLE : View.GONE);
        vh.repostChannelView.setText(isRepost && original.getSigningChannel() != null ? original.getSigningChannel().getName() : null);
        vh.repostChannelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onClaimClicked(original.getSigningChannel());
                }
            }
        });

        if (vh.optionsMenuView != null) {
            vh.optionsMenuView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setPosition(vh.getBindingAdapterPosition());
                    view.showContextMenu();
                }
            });
        }

        vh.titleView.setText(Helper.isNullOrEmpty(item.getTitle()) ? item.getName() : item.getTitle());
        if (type == VIEW_TYPE_FEATURED) {
            LbryUri vanityUrl = new LbryUri();
            vanityUrl.setClaimName(item.getName());
            vh.vanityUrlView.setText(vanityUrl.toString());
        }

        vh.feeContainer.setVisibility(item.isUnresolved() || !Claim.TYPE_STREAM.equalsIgnoreCase(item.getValueType()) ? View.GONE : View.VISIBLE);
        vh.noThumbnailView.setVisibility(Helper.isNullOrEmpty(thumbnailUrl) ? View.VISIBLE : View.GONE);
        Helper.setIconViewBackgroundColor(vh.noThumbnailView, bgColor, false, context);

        Helper.setViewVisibility(vh.loadingImagePlaceholder, item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.loadingTextPlaceholder1, item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.loadingTextPlaceholder2, item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.titleView, !item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.publisherView, !item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.publishTimeView, !item.isLoadingPlaceholder() && !isPending ? View.VISIBLE : View.GONE);

        if (type == VIEW_TYPE_FEATURED && item.isUnresolved()) {
            vh.durationView.setVisibility(View.GONE);
            vh.titleView.setText("Nothing here. Publish something!");
            String name = item.getName();
            if (!Helper.isNullOrEmpty(name)) {
                vh.alphaView.setText(name.substring(0, Math.min(5, name.length() - 1)));
            }
        } else {
            if (Claim.TYPE_STREAM.equalsIgnoreCase(item.getValueType()) || Claim.TYPE_COLLECTION.equalsIgnoreCase(item.getValueType())) {
                if (!Helper.isNullOrEmpty(thumbnailUrl)) {
                    Glide.with(context.getApplicationContext()).
                            asBitmap().
                            load(thumbnailUrl).
                            placeholder(R.drawable.bg_thumbnail_placeholder).
                            into(vh.thumbnailView);
                    vh.thumbnailView.setVisibility(View.VISIBLE);
                } else {
                    vh.thumbnailView.setVisibility(View.GONE);
                }

                BigDecimal cost = item.getActualCost(Lbryio.LBCUSDRate);
                vh.feeContainer.setVisibility(cost.doubleValue() > 0 && !hideFee ? View.VISIBLE : View.GONE);
                vh.feeView.setText(cost.doubleValue() > 0 ? Helper.shortCurrencyFormat(cost.doubleValue()) : "Paid");
                vh.alphaView.setText(item.getName().substring(0, Math.min(5, item.getName().length() - 1)));
                vh.publisherView.setText(signingChannel != null ? signingChannel.getName() : context.getString(R.string.anonymous));
                vh.publishTimeView.setText(DateUtils.getRelativeTimeSpanString(
                        publishTime, System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE));
                long duration = item.getDuration();
                vh.durationView.setVisibility((duration > 0 || item.isLive() || Claim.TYPE_COLLECTION.equalsIgnoreCase(item.getValueType())) ? View.VISIBLE : View.GONE);
                if (item.isLive()) {
                    vh.durationView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                    vh.durationView.setText(context.getResources().getString(R.string.live).toUpperCase());
                } else {
                    vh.durationView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black));
                    if (!Claim.TYPE_COLLECTION.equalsIgnoreCase(item.getValueType())) {
                        vh.durationView.setText(Helper.formatDuration(duration));
                        vh.durationView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    } else {
                        vh.durationView.setText(String.valueOf(item.getClaimIds().size()));
                        vh.durationView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_list_icon, 0, 0, 0);
                        vh.durationView.setCompoundDrawablePadding(8);
                    }
                }

                LbryFile claimFile = item.getFile();
                boolean isDownloading = false;
                int progress = 0;
                String fileSizeString = claimFile == null ? null : Helper.formatBytes(claimFile.getTotalBytes(), false);
                if (claimFile != null &&
                        !Helper.isNullOrEmpty(claimFile.getDownloadPath()) &&
                        !claimFile.isCompleted() &&
                        claimFile.getWrittenBytes() < claimFile.getTotalBytes()) {
                    isDownloading = true;
                    progress = claimFile.getTotalBytes() > 0 ?
                            Double.valueOf(((double) claimFile.getWrittenBytes() / (double) claimFile.getTotalBytes()) * 100.0).intValue() : 0;
                    fileSizeString = String.format("%s / %s",
                            Helper.formatBytes(claimFile.getWrittenBytes(), false),
                            Helper.formatBytes(claimFile.getTotalBytes(), false));
                }

                Helper.setViewText(vh.fileSizeView, claimFile != null && !Helper.isNullOrEmpty(claimFile.getDownloadPath()) ? fileSizeString : null);
                Helper.setViewVisibility(vh.downloadProgressView, isDownloading ? View.VISIBLE : View.INVISIBLE);
                Helper.setViewProgress(vh.downloadProgressView, progress);
                Helper.setViewText(vh.deviceView, item.getDevice());
            } else if (Claim.TYPE_CHANNEL.equalsIgnoreCase(item.getValueType())) {
                if (!Helper.isNullOrEmpty(thumbnailUrl)) {
                    Glide.with(context.getApplicationContext()).
                            load(thumbnailUrl).
                            centerCrop().
                            placeholder(R.drawable.bg_thumbnail_placeholder).
                            apply(RequestOptions.circleCropTransform()).
                            into(vh.thumbnailView);
                }
                vh.alphaView.setText(item.getName().substring(1, 2).toUpperCase());
                vh.publisherView.setText(item.getName());
                vh.publishTimeView.setText(DateUtils.getRelativeTimeSpanString(
                        publishTime, System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE));
            }
        }
    }

    private void toggleSelectedClaim(Claim claim) {
        if (selectedItems.contains(claim)) {
            selectedItems.remove(claim);
        } else {
            selectedItems.add(claim);
        }

        if (selectionModeListener != null) {
            selectionModeListener.onItemSelectionToggled();
        }

        if (selectedItems.size() == 0) {
            inSelectionMode = false;
            if (selectionModeListener != null) {
                selectionModeListener.onExitSelectionMode();
            }
        }

        notifyDataSetChanged();
    }

    public interface ClaimListItemListener {
        void onClaimClicked(Claim claim);
    }
}

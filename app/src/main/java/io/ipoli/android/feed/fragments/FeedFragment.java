package io.ipoli.android.feed.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.ipoli.android.Constants;
import io.ipoli.android.MainActivity;
import io.ipoli.android.R;
import io.ipoli.android.app.App;
import io.ipoli.android.app.BaseFragment;
import io.ipoli.android.app.events.EventSource;
import io.ipoli.android.app.ui.LayoutManagerFactory;
import io.ipoli.android.app.ui.dialogs.DateTimePickerFragment;
import io.ipoli.android.app.utils.NetworkConnectivityUtils;
import io.ipoli.android.app.utils.ViewUtils;
import io.ipoli.android.feed.data.Post;
import io.ipoli.android.feed.persistence.FeedPersistenceService;
import io.ipoli.android.feed.ui.PostBinder;
import io.ipoli.android.feed.ui.PostViewHolder;
import io.ipoli.android.player.CredentialStatus;
import io.ipoli.android.player.data.Player;
import io.ipoli.android.player.PlayerCredentialChecker;
import io.ipoli.android.player.PlayerCredentialsHandler;
import io.ipoli.android.player.activities.ProfileActivity;
import io.ipoli.android.player.events.AddKudosEvent;
import io.ipoli.android.player.events.CreateQuestFromPostEvent;
import io.ipoli.android.player.events.RemoveKudosEvent;
import io.ipoli.android.player.persistence.PlayerPersistenceService;
import io.ipoli.android.quest.activities.QuestPickerActivity;
import io.ipoli.android.quest.data.Quest;
import io.ipoli.android.quest.events.NewQuestEvent;
import io.ipoli.android.quest.persistence.QuestPersistenceService;

import static io.ipoli.android.feed.persistence.FirebaseFeedPersistenceService.postsPath;

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 6/28/17.
 */

public class FeedFragment extends BaseFragment {

    @Inject
    QuestPersistenceService questPersistenceService;

    @Inject
    FeedPersistenceService feedPersistenceService;

    @Inject
    PlayerPersistenceService playerPersistenceService;

    @Inject
    PlayerCredentialsHandler playerCredentialsHandler;

    @BindView(R.id.root_container)
    ViewGroup rootContainer;

    @BindView(R.id.loader)
    ProgressBar loader;

    @BindView(R.id.feed_list)
    RecyclerView feedList;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private FirebaseRecyclerAdapter<Post, PostViewHolder> adapter;

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_feed, container, false);
        unbinder = ButterKnife.bind(this, view);
        App.getAppComponent(getContext()).inject(this);
        ((MainActivity) getActivity()).initToolbar(toolbar, R.string.title_fragment_feed);

        loader.setVisibility(View.VISIBLE);

        feedList.setLayoutManager(LayoutManagerFactory.createReverseLayoutManager(getContext()));

        DatabaseReference postsReference = postsPath().toReference();
        adapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(Post.class,
                R.layout.feed_post_item,
                PostViewHolder.class,
                postsReference.limitToLast(100)) {
            @Override
            protected void populateViewHolder(PostViewHolder holder, Post post, int position) {
                int marginBottom = position == 0 ? 92 : 4;
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
                lp.bottomMargin = (int) ViewUtils.dpToPx(marginBottom, getResources());
                holder.itemView.setLayoutParams(lp);

                PostBinder.bind(holder, post, getPlayerId());

                holder.giveKudosContainer.setOnClickListener(v -> onLikePost(post));
                holder.addQuestContainer.setOnClickListener(v -> onAddQuest(post));
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), ProfileActivity.class);
                    intent.putExtra(Constants.PLAYER_ID_EXTRA_KEY, post.getPlayerId());
                    startActivity(intent);
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (loader.getVisibility() == View.VISIBLE) {
                    loader.setVisibility(View.GONE);
                }
            }
        };

        feedList.setAdapter(adapter);

        return view;
    }

    private void onAddQuest(Post post) {
        if (!NetworkConnectivityUtils.isConnectedToInternet(getContext())) {
            Toast.makeText(getContext(), R.string.enable_internet_to_do_action, Toast.LENGTH_LONG).show();
            return;
        }

        Player player = getPlayer();
        CredentialStatus credentialStatus = PlayerCredentialChecker.checkStatus(player);
        if (credentialStatus != CredentialStatus.AUTHORIZED) {
            playerCredentialsHandler.authorizeAccess(player, credentialStatus, PlayerCredentialsHandler.Action.ADD_QUEST,
                    (AppCompatActivity) getActivity(), rootContainer);
            return;
        }

        if (!post.isAddedByPlayer(player.getId())) {
            feedPersistenceService.addPostToPlayer(post, player.getId());
        }

        DateTimePickerFragment.newInstance(player.getUse24HourFormat(), (date, time) -> {
            Quest quest = new Quest(post.getTitle(), date, post.getCategoryType());
            quest.setDuration(post.getDuration());
            quest.setStartTime(time);
            postEvent(new NewQuestEvent(quest, EventSource.FEED));
            Toast.makeText(getContext(), R.string.quest_from_post_added, Toast.LENGTH_LONG).show();
        }).show(getFragmentManager());
        postEvent(new CreateQuestFromPostEvent(post, EventSource.FEED));
    }

    private void onLikePost(Post post) {
        if (!NetworkConnectivityUtils.isConnectedToInternet(getContext())) {
            Toast.makeText(getContext(), R.string.enable_internet_to_do_action, Toast.LENGTH_LONG).show();
            return;
        }

        Player player = getPlayer();
        CredentialStatus credentialStatus = PlayerCredentialChecker.checkStatus(player);
        if (credentialStatus != CredentialStatus.AUTHORIZED) {
            playerCredentialsHandler.authorizeAccess(player, credentialStatus, PlayerCredentialsHandler.Action.GIVE_KUDOS,
                    (AppCompatActivity) getActivity(), rootContainer);
            return;
        }
        if (post.isGivenKudosByPlayer(player.getId())) {
            feedPersistenceService.removeKudos(post, player.getId());
            postEvent(new RemoveKudosEvent(post, EventSource.FEED));
        } else {
            feedPersistenceService.addKudos(post, player.getId());
            postEvent(new AddKudosEvent(post, EventSource.FEED));
        }
    }

    @OnClick(R.id.add_quest_to_feed)
    public void onAddQuestToFeed(View v) {
        if (!NetworkConnectivityUtils.isConnectedToInternet(getContext())) {
            Toast.makeText(getContext(), R.string.enable_internet_to_do_action, Toast.LENGTH_LONG).show();
            return;
        }

        Player player = getPlayer();
        CredentialStatus credentialStatus = PlayerCredentialChecker.checkStatus(player);
        if (credentialStatus != CredentialStatus.AUTHORIZED) {
            playerCredentialsHandler.authorizeAccess(player, credentialStatus, PlayerCredentialsHandler.Action.SHARE_QUEST,
                    (AppCompatActivity) getActivity(), rootContainer);
            return;
        }
        startActivity(new Intent(getContext(), QuestPickerActivity.class));
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        adapter.cleanup();
        super.onDestroyView();
    }

    @Override
    protected boolean useOptionsMenu() {
        return false;
    }
}
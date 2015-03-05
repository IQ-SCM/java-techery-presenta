/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mortar.core;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.mortar.R;
import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.flow.GsonParceler;
import com.example.mortar.flow.pathview.HandlesBack;
import com.example.mortar.screen.ChatListScreen;
import com.example.mortar.screen.FriendListScreen;
import com.google.gson.Gson;

import javax.inject.Inject;

import flow.ActivityFlowSupport;
import flow.Backstack;
import flow.Flow;
import flow.Path;
import flow.PathContainerView;
import mortar.MortarScope;
import mortar.MortarScopeDevHelper;
import mortar.bundler.BundleServiceRunner;
import mortar.dagger1support.ObjectGraphService;
import rx.functions.Action0;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static mortar.bundler.BundleServiceRunner.getBundleServiceRunner;

/**
 * A well intentioned but overly complex sample that demonstrates
 * the use of Mortar, Flow and Dagger in a single app.
 */
public class MortarDemoActivity extends android.app.Activity
    implements ActionBarOwner.Activity, Flow.Dispatcher {

  private MortarScope activityScope;
  private ActionBarOwner.MenuAction actionBarMenuAction;

  @Inject
  ActionBarOwner actionBarOwner;

  private PathContainerView container;
  private HandlesBack containerAsHandlesBack;
  private ActivityFlowSupport flowSupport;

  @Override
  public Context getContext() {
    return this;
  }

  @Override
  public void dispatch(Flow.Traversal traversal, final Flow.TraversalCallback callback) {
    Path path = traversal.destination.current();
    setTitle(path.getClass().getSimpleName());
    boolean canGoBack = traversal.destination.size() > 1;
    String title = path.getClass().getSimpleName();
    ActionBarOwner.MenuAction menu = canGoBack ? null : new ActionBarOwner.MenuAction("Friends", new Action0() {
          @Override
          public void call() {
            Flow.get(MortarDemoActivity.this).goTo(new FriendListScreen());
          }
        });
    actionBarOwner.setConfig(new ActionBarOwner.Config(false, canGoBack, title, menu));
    container.dispatch(traversal, new Flow.TraversalCallback() {
      @Override
      public void onTraversalCompleted() {
        invalidateOptionsMenu();
        callback.onTraversalCompleted();
      }
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ActivityFlowSupport.NonConfigurationInstance nonConfigurationInstance = (ActivityFlowSupport.NonConfigurationInstance) getLastNonConfigurationInstance();
    Backstack backstack = Backstack.single(new ChatListScreen());
    flowSupport = ActivityFlowSupport.onCreate(nonConfigurationInstance, savedInstanceState, new GsonParceler(new Gson()), backstack);
    MortarScope parentScope = MortarScope.getScope(getApplication());

    String scopeName = getLocalClassName() + "-task-" + getTaskId();
    activityScope = parentScope.findChild(scopeName);
    if (activityScope == null) {
      activityScope = parentScope.buildChild(scopeName)
          .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
          .build();
    }
    BundleServiceRunner.getBundleServiceRunner(activityScope).onCreate(savedInstanceState);
    ObjectGraphService.inject(this, this);

    actionBarOwner.takeView(this);

    setContentView(R.layout.root_layout);
    container = (PathContainerView) findViewById(R.id.container);
    containerAsHandlesBack = (HandlesBack) container;
  }

  @Override
  protected void onResume() {
    super.onResume();
    flowSupport.onResume(this);
  }

  @Override
  protected void onPause() {
    flowSupport.onPause();
    super.onPause();
  }

  @Override
  public Object getSystemService(String name) {
    Object flow = null;
    if (flowSupport != null) flow = flowSupport.getSystemService(name);
    return flow != null ? flow : (activityScope != null && activityScope.hasService(name)) ? activityScope.getService(name) : super.getSystemService(name);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    flowSupport.onSaveInstanceState(outState);
    getBundleServiceRunner(this).onSaveInstanceState(outState);
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    return flowSupport.onRetainNonConfigurationInstance();
  }

  /**
   * Inform the view about back events.
   */
  @Override
  public void onBackPressed() {
    if (!containerAsHandlesBack.onBackPressed() && !flowSupport.onBackPressed()) super.onBackPressed();
  }

  /**
   * Inform the view about up events.
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Configure the action bar menu as required by {@link ActionBarOwner.Activity}.
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (actionBarMenuAction != null) {
      menu.add(actionBarMenuAction.title)
          .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
              actionBarMenuAction.action.call();
              return true;
            }
          });
    }
    menu.add("Log Scope Hierarchy")
        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            Log.d("DemoActivity", MortarScopeDevHelper.scopeHierarchyToString(activityScope));
            return true;
          }
        });
    return true;
  }

  @Override
  protected void onDestroy() {
    actionBarOwner.dropView(this);

    // activityScope may be null in case isWrongInstance() returned true in onCreate()
    if (isFinishing() && activityScope != null) {
      activityScope.destroy();
      activityScope = null;
    }

    super.onDestroy();
  }

  @Override
  public void setShowHomeEnabled(boolean enabled) {
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);
  }

  @Override
  public void setUpButtonEnabled(boolean enabled) {
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(enabled);
    actionBar.setHomeButtonEnabled(enabled);
  }

  @Override
  public void setTitle(CharSequence title) {
    getActionBar().setTitle(title);
  }

  @Override
  public void setMenu(ActionBarOwner.MenuAction action) {
    if (action != actionBarMenuAction) {
      actionBarMenuAction = action;
      invalidateOptionsMenu();
    }
  }

}

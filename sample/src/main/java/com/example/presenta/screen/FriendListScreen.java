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
package com.example.presenta.screen;

import android.os.Bundle;
import com.example.presenta.R;
import com.example.presenta.model.Chats;
import com.example.presenta.model.User;
import com.example.presenta.flow.HasParent;
import com.example.presenta.view.FriendListView;
import flow.Flow;
import flow.path.Path;
import io.techery.presenta.addition.flow.path.Layout;
import io.techery.presenta.mortarscreen.presenter.InjectablePresenter;
import io.techery.presenta.mortarscreen.presenter.WithPresenter;
import java.util.List;
import javax.inject.Inject;

@Layout(R.layout.friend_list_view) @WithPresenter(FriendListScreen.Presenter.class)
public class FriendListScreen extends Path implements HasParent {

  public static class Presenter extends InjectablePresenter<FriendListView> {
    List<User> friends;
    @Inject Chats service;

    public Presenter(PresenterInjector injector) {
      super(injector);
      this.friends = service.getFriends();
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      if (!hasView()) return;
      getView().showFriends(friends);
    }

    public void onFriendSelected(int position) {
      Flow.get(getView()).set(new FriendScreen(position));
    }
  }

  @Override public ChatListScreen getParent() {
    return new ChatListScreen();
  }
}

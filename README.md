# AntViewer-android

[![](https://jitpack.io/v/antourage/AntViewer-android.svg)](https://jitpack.io/#antourage/AntViewer-android)

## Functional Description

The Antourage Widget is designed to work as a self-contained ‘widget’ within a host app. It shows live broadcasts and on-demand videos which have been captured by our mobile Broadcaster application. Antourage is mobile first and designed for the creation and viewing of realtime and near realtime micro-content.

### Magnetic Button

The entry point for a user is the magnetic button that appears on the host app, usually on the main screen.

When a video is live, the button changes state and shows the title of the broadcast, and the name of the broadcaster. If the user taps the button at this point, they are taken directly to the live broadcast.

<img src="https://user-images.githubusercontent.com/52660451/72547997-75c02d00-3896-11ea-83e7-ccb1b7af69f0.jpg" width="200" />

When no video is live, the button shows how many on-demand videos the viewer has not yet watched. If they tap the button at this point, they are taken to the main view:

<img src="https://user-images.githubusercontent.com/52660451/72547993-75c02d00-3896-11ea-8701-26bfa1981ecd.jpg" width="200" />

### Viewing Live Broadcasts

The video player may be used in portrait or landscape mode. In both modes, the viewer can watch the broadcast, see and contribute to comments, and see and respond to polls.

<div>
  <img src="https://user-images.githubusercontent.com/52660451/72547996-75c02d00-3896-11ea-84dc-e7c6b5976174.jpg" alt="Screenshots" width="200" />
  <img src="https://user-images.githubusercontent.com/52660451/72547994-75c02d00-3896-11ea-99a5-12b378401bf6.jpg" alt="Screenshots" width="400" />
</div>

### Viewing On-demand videos

When the user taps on a video, the video begins playing at the beginning, or if this video has already been partially viewed, it will begin playing at the last point the viewer watched. The Antourage Widget keeps track of which videos the user has seen, and updates the number on the magnetic button accordingly.

Each video shows the name of the video, name of the broadcaster, total time, and total view count.

### Display Name

In order to contribute to the comments, a user must have an identity in our system, as well as a Display Name that shows in the comments stream. Since not all host apps require a username, we ask users to create a Display Name the first time they try to chat. If the host app does require users to create a username, we can turn off this feature.

### Comments

Comments are contributed by viewers of the live broadcast only. When a video is being watched later, these comments may be displayed, but cannot be added to. The broadcaster has the ability to review comments on a video and delete ones that they deem to be unacceptable. Antourage administration also has this ability.

### Polls

Polls are created by the broadcaster, and sent out during a live broadcast. They appear on the screen when they are first pushed out to the audience, and viewers can respond or simply close the poll if they do not want to answer. If they answer, they are shown the results right away, and they can see updated results as they come in.

<img src="https://user-images.githubusercontent.com/52660451/72547995-75c02d00-3896-11ea-87da-8c48361546ac.jpg" alt="Screenshots" width="200" />

### Curation

Content can only be created by those who have been actively selected by our customers to broadcast, and broadcasters can remove their own content from view at any time. Each customer designates a time period for the storage and display of their content — usually between 2 weeks and 6 weeks. After the storage period is over, content is automatically removed. These three elements together — actively selected creators, video removal, and video aging — ensure that the viewers get a consistent experience of interesting and fresh content.


### Third Party Technology

To support our functionality, we use a few third-party services and applications.   
Firebase: used for push notifications, comments and polls.   
Amazon Media Live: used for streaming and hosting our content

## Compatibility

* Minimum Android SDK: Antourage widget requires a minimum API level of 21;
* Starting from 0.2.0 version library has migrated to the androidX, so make sure that you have

```gradle
android.useAndroidX=true
android.enableJetifier=true
```
in your gradle.properties

If your app doesn't support androidX, please use 0.1.5 version of the widget library;

## Setup

*Add:
```gradle
    compileOptions {
        targetCompatibility 1.8
        sourceCompatibility 1.8
    }
```

and

```gradle
dependencies {
    ...
    implementation 'com.github.antourage:AntViewer-android:0.2.11'
}
```


## Usage

Add antourage widget view to the layout:

```xml
    <com.antourage.weaverlib.ui.fab.AntourageFab
            android:id="@+id/antfab"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintRight_toRightOf="parent" />
```

Make sure that layout_width attribute value is set to "match_parent".

### Auth

```kotlin
        antfab.authWith(TEST_API_KEY.toUpperCase(), callback = { userAuthResult ->
            when (userAuthResult) {
                is UserAuthResult.Success -> {
                    Log.d(TAG, "Ant authorization successful!")
                }
                is UserAuthResult.Failure -> {
                    Log.e(TAG, "Ant authorization failed because: ${userAuthResult.cause}")
                }
            }
        })

```

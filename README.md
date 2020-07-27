# Antourage SDK Functional Description

[![](https://jitpack.io/v/antourage/AntViewer-android.svg)](https://jitpack.io/#antourage/AntViewer-android)

<img src="https://user-images.githubusercontent.com/52660451/88295546-3ae7f780-cd06-11ea-9959-2c8800709214.png" width="200" />

The Antourage Widget is designed to work as a self-contained ‘widget’ within a host app. Once opened, the widget launches a micro-content vertical that includes live broadcasts and on-demand videos. This content is captured by our mobile Broadcaster application. Antourage is mobile first and designed for the creation and viewing of realtime and near real time micro-content.

<img src="https://user-images.githubusercontent.com/52660451/88295016-8d74e400-cd05-11ea-945b-0e300297ee16.png" width="600" />

### Magnetic Widget

The entry point for a user is the magnetic button that appears on the host app. Usually on the main screen, but flexible, the button can appear in more than one place. This magnetic widget can appear in multiple states.

**“Resting”**

<img src="https://user-images.githubusercontent.com/52660451/88295341-f6f4f280-cd05-11ea-8132-cad856cad12d.png" width="100" />

If there are no live videos or new VOD’s to watch the widget will be in a “resting” state. When a user clicks the widget in its resting state, they are directed to the main menu of the widget.

**“LIVE”**

<div>
  <img src="https://user-images.githubusercontent.com/52660451/88295455-1d1a9280-cd06-11ea-9e3d-71909a207938.gif"  width="150" />
  <img src="https://user-images.githubusercontent.com/52660451/88295546-3ae7f780-cd06-11ea-9959-2c8800709214.png"  width="100" />
  <img src="https://user-images.githubusercontent.com/52660451/88295584-49361380-cd06-11ea-8697-8b21a6216d65.png"  width="100" />
</div>

When a broadcaster starts streaming live video, the button changes state and animates. The live video can be seen inside the widget and “LIVE” tag appears. If a user taps the widget whilst in this state, they are taken directly to the live broadcast.

**“NEW”**

<img src="https://user-images.githubusercontent.com/52660451/88295639-5bb04d00-cd06-11ea-9b96-7e7596696d02.gif" width="150" />

When there isn’t a live video, but there are unwatched VOD’s the widget animates with a “NEW” tag. If a user clicks the widget at this point, they will subsequently see the main menu.

**The Main Menu**

The main menu allows the user to navigate through multiple live and new videos. Whilst navigating through the videos, if they stop scolling a video will play without sound.

If a user clicks on the comment or poll icon below any video they will be taken directly to the chat or poll within that video so that they can contribute immediately.

The main menu can also be customised, by editing the logo in the corner of the screen to surface the organisation or sponsors. The title of the menu can also be customised.

<img src="https://user-images.githubusercontent.com/52660451/88294908-71714280-cd05-11ea-8723-c7328fe898d9.png" width="200" />

### Viewing Live Broadcasts

The video player may be used in portrait or landscape mode. In both modes, the viewer can watch the broadcast, see and contribute to comments, and see and respond to polls.

<div>
  <img src="https://user-images.githubusercontent.com/52660451/88294932-7930e700-cd05-11ea-9d30-83dbb172dbfb.png" width="200" />
  <img src="https://user-images.githubusercontent.com/52660451/88377784-7804c600-cda8-11ea-8a5f-371f5cffcc43.png" width="400" />
</div>

### Viewing On-demand videos

When the user taps on a video, the video begins playing at the beginning, or if this video has already been partially viewed, it will begin playing at the last point the viewer watched. The Antourage Widget keeps track of which videos the user has seen, and updates the number on the magnetic button accordingly.

Each video shows the name of the video, name of the broadcaster, total time, and total view count.

### Display Name

In order to contribute to the comments, a user must have an identity in our system, as well as a Display Name that shows in the comments stream. Since not all host apps require a username, we ask users to create a Display Name the first time they try to chat. If the host app does require users to create a username, we can turn off this feature.

### Comments

Comments are contributed by viewers of the live broadcast only. When a video is being watched later as VOD, these comments may be displayed, but cannot be added to. The broadcaster has the ability to review comments on a video and delete ones that they deem to be unacceptable. Antourage administration also has this ability.

### Polls

Polls are created by the broadcaster, and sent out during a live broadcast. They appear on the screen when they are first pushed out to the audience, and viewers can respond or simply close the poll if they do not want to answer. If they answer, they are shown the results right away, and they can see updated results as they come in.

These polls are sponsorable and images can be uploaded from the web application so that they surface on behalf of all broadcasters. This uploaded images can also be clickable and link to web pages for special offers or further sponsor activation.

<img src="https://user-images.githubusercontent.com/52660451/88295057-9a91d300-cd05-11ea-8cae-18f2a472e3a6.png" width="200" />

### The Curtain

The curtain feature is supposed to mimic the purpose of a curtain at the theatre. To serve a business purpose such as sponsor exposure or ticket sales, a curtain be lowered at any time. Alternatively, a user can also use the curtain to hide what they are streaming whilst they prepare simulcasts or perform duties off camera.

Multiple curtains can be uploaded at the same time, therefore different messages/sponsors that you can be ready to raise awareness of when ready. 

<img src="https://user-images.githubusercontent.com/52660451/88295697-6f5bb380-cd06-11ea-986a-d76e2486acde.png" width="400" />

### Curation

Content can only be created by those who have been actively been given access to stream by the administrator of our partner. Furthermore, with this access, broadcasters can only stream to the specific channels that they have been granted access to stream to. 

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
    implementation 'com.github.antourage:AntViewer-android:0.2.15'
}
```


## Usage

Add antourage widget view to the layout:

```xml
    <com.antourage.weaverlib.ui.fab.AntourageFab
            android:id="@+id/antfab"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintRight_toRightOf="parent" />
```

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
## Author

Nazar Mykhailevych, nm@leobit.com

## License

AntViewer is available under the MIT license. See the LICENSE file for more info.

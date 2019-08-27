package com.antourage.weaverlib.di

import android.app.Application
import com.antourage.weaverlib.screens.list.VideoListViewModel
import com.antourage.weaverlib.screens.poll.PollDetailsViewModel
import com.antourage.weaverlib.screens.vod.VideoViewModel
import com.antourage.weaverlib.screens.weaver.PlayerViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/**
 * I like to consider that as the Dagger configuration entry point. It unfortunately doesn't look
 * simple (even though it doesn't look that bad either) and it doesn't get much better than that.
 */
@Singleton
@Component(modules = [RetrofitModule::class])
interface ApplicationComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }

    /**
     * We could've chosen to create an inject() method instead and do field injection in the
     * Activity, but for this case this seems less verbose to me in the end.
     */
    fun getVideoListViewModelFactory(): ViewModelFactory<VideoListViewModel>

    fun getVideoViewModelFactory(): ViewModelFactory<VideoViewModel>

    fun getWeaverViewModelFactory(): ViewModelFactory<PlayerViewModel>

    fun getPollViewModelFactory(): ViewModelFactory<PollDetailsViewModel>

}

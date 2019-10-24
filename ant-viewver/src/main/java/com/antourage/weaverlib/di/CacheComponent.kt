package com.antourage.weaverlib.di

import com.antourage.weaverlib.ui.fab.AntourageFab
import dagger.Component

@Component
interface CacheComponent{
    fun injectCache(fab: AntourageFab)
}
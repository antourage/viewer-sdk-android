package com.antourage.weaverlib.other

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int,addToBackStack:Boolean = false) {
    if(addToBackStack){
        supportFragmentManager.inTransaction{
            replace(frameId, fragment).addToBackStack(fragment.javaClass.simpleName)}
    } else
        supportFragmentManager.inTransaction{
            replace(frameId, fragment)}
}
fun Fragment.replaceFragment(fragment: Fragment, frameId: Int,addToBackStack:Boolean = false) {
    (activity as AppCompatActivity).replaceFragment(fragment,frameId,addToBackStack)
}

fun Fragment.replaceChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction{replace(frameId, fragment)}
}
fun <T> LiveData<T>.observeSafe(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<in T>) {
    this.removeObserver(observer)
    this.observe(owner, observer)
}
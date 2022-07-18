package com.example.android.instagramclone.main

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.android.instagramclone.IgViewModel

@Composable

fun FeedScreen(navController: NavController, vm:IgViewModel){
    Text(text = "feed screen")
}
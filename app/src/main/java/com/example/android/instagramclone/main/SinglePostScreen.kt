package com.example.android.instagramclone.main

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.android.instagramclone.IgViewModel
import com.example.android.instagramclone.data.PostData

@Composable
fun SinglePostScreen(navController: NavController, vm: IgViewModel, post:PostData){
    Text(text = "Single post Screen ${post.postDescription}")
}
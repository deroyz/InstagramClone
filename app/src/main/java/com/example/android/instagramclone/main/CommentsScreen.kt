package com.example.android.instagramclone.main

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.android.instagramclone.IgViewModel

@Composable
fun CommentsScreen(navController: NavController, vm: IgViewModel, postId: String){
    Text(text = "comments screen")
}
package com.example.android.instagramclone.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.android.instagramclone.DestinationScreen
import com.example.android.instagramclone.IgViewModel
import com.example.android.instagramclone.main.navigateTo

@Composable
fun LoginScreen(navController: NavController, vm: IgViewModel){
    Text(
        text = "New Here? Go to signup ->",
        color = Color.Blue,
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                navigateTo(navController, DestinationScreen.Signup)
            }
    )
}
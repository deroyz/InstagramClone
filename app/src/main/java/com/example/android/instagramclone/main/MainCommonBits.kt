package com.example.android.instagramclone.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.android.instagramclone.IgViewModel

@Composable
fun NotificationMessage(vm: IgViewModel){
    val notifState = vm.popupNotification.value
    val notifMessage = notifState?.getContentOrNull()
    if( notifMessage != null){
        Toast.makeText(LocalContext.current, notifMessage, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun CommonProgressSpinner(){
    Row(
      modifier = Modifier
          .alpha(0.5f)
          .background(Color.LightGray)
          .clickable(enabled = false) { }
          .fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ){
        CircularProgressIndicator()
    }
}
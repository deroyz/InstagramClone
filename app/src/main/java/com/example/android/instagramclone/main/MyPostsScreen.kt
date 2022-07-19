package com.example.android.instagramclone.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.android.instagramclone.DestinationScreen
import com.example.android.instagramclone.IgViewModel
import com.example.android.instagramclone.R

@Composable
fun MyPostScreen(navController: NavController, vm: IgViewModel) {

    val newPostImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let {
                val encoded = Uri.encode(uri.toString())
                val route = DestinationScreen.NewPost.createRoute(encoded)
                navController.navigate(route)
            }
        }

    val userData = vm.userData.value
    val isLoading = vm.inProgress.value

    Column {

        Column(modifier = Modifier.weight(1f)) {

            Row() {

                ProfileImage(userData?.imageUrl) {
                    newPostImageLauncher.launch("image/*")
                }

                Text(
                    text = "15\nposts",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "55\nfollowers",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "93\nfollowing",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )
            }

            Column(modifier = Modifier.padding(8.dp)) {
                val usernameDisplay =
                    if (userData?.username == null) "" else "@${userData.username}"
                Text(text = userData?.name ?: "", fontWeight = FontWeight.Bold)
                Text(text = usernameDisplay)
                Text(text = userData?.bio ?: "")
            }

            OutlinedButton(
                onClick = { navigateTo(navController, DestinationScreen.Profile) },
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                ),
                shape = RoundedCornerShape(10)
            ) {
                Text(text = "Edit Profile", color = Color.Black)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Posts list")
            }

        }


        BottomNavigationMenu(
            selectedItem = BottomNavigationItem.POSTS,
            navController = navController
        )
    }

    if (isLoading) {
        CommonProgressSpinner()
    }
}


@Composable
fun ProfileImage(imageUrl: String?, onClick: () -> Unit) {
    Box(modifier = Modifier
        .clickable { onClick.invoke() }
        .padding(top = 16.dp)) {

        UserImageCard(
            userImage = imageUrl, modifier = Modifier
                .padding(8.dp)
                .size(80.dp)
        )

        Card(
            shape = CircleShape,
            border = BorderStroke(
                width = 2.dp,
                color = Color.White,
            ),
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.background(Color.Blue)
            )
        }
    }
}

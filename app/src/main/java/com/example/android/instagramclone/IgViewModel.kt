package com.example.android.instagramclone

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.android.instagramclone.data.CommentData
import com.example.android.instagramclone.data.Event
import com.example.android.instagramclone.data.PostData
import com.example.android.instagramclone.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlin.Exception

const val USERS = "users"
const val POSTS = "posts"
const val COMMENTS = "comments"

@HiltViewModel
class IgViewModel @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseFirestore,
    val storage: FirebaseStorage,
) : ViewModel() {

    val signedIn = mutableStateOf(false)
    val inProgress = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)
    val popupNotification = mutableStateOf<Event<String>?>(null)

    val refreshPostsProgress = mutableStateOf(false)
    val posts = mutableStateOf<List<PostData>>(listOf())

    val searchedPosts = mutableStateOf<List<PostData>>(listOf())
    val searchedPostsProgress = mutableStateOf(false)

    val postsFeed = mutableStateOf<List<PostData>>(listOf())
    val postsFeedProgress = mutableStateOf(false)

    val comments = mutableStateOf<List<CommentData>>(listOf())
    val commentsProgress = mutableStateOf(false)

    val followers = mutableStateOf(0)

    init {
//        auth.signOut()
        val currentUser = auth.currentUser
        signedIn.value = currentUser != null
        currentUser?.uid?.let { uid ->
            getUserData(uid)
        }
    }

    // Method for signup
    fun onSignup(username: String, email: String, pass: String) {

        // Check if input field for user name or email or password is empty
        if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            handleException(customMessage = "Please fill in all fields")
            return
        }

        // If all the fields are filled, it goes into the signup process
        inProgress.value = true

        // Attempt to get the user data from "USERS" collection on Firestore Database with input user name
        db.collection(USERS).whereEqualTo("username", username).get()

            .addOnSuccessListener { documents ->
                // Case username already exist
                if (documents.size() > 0) {
                    handleException(customMessage = "Username already exist")
                    // Process done
                    inProgress.value = false

                // Case input userdata does not exist
                } else {
                    // Create new user account with input email and password
                    auth.createUserWithEmailAndPassword(email, pass)
                        // Inquiry creating account with email and password is complete (could be successful or not)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Let ViewModel notice that it is signed in with signup attempted  username
                                signedIn.value = true
                                // Create new userdata with username and save to "USERS" collection on Firestore Database
                                createOrUpdateProfile(username = username)
                            } else {
                                handleException(task.exception, "Signup failed")
                            }
                            // Process done
                            inProgress.value = false
                        }
                }
            }

            // Case not able to receive the document from database
            .addOnFailureListener { exc ->
                // Process done
                inProgress.value = false
                handleException(exc, "Signup failed")
            }
    }

    // Method for login
    fun onLogin(email: String, pass: String) {
        // Check if input fields(email and password) are empty
        if (email.isEmpty() || pass.isEmpty()) {
            handleException(customMessage = "Please fill in all fields")
            return
        }
        // If fields are filled up properly start login process
        inProgress.value = true

        // Sign in to Firebase Authentication using filled up information (email and password)
        auth.signInWithEmailAndPassword(email, pass)

            // Inquiry signing in is complete (could be successful or not)
            .addOnCompleteListener { task ->
                // Case signed in properly
                if (task.isSuccessful) {
                    // Sign in value turns to true
                    signedIn.value = true
                    // Process done
                    inProgress.value = false
                    // Retrieve user data by calling getUserData method with uid on Firebase Authentication
                    auth.currentUser?.uid?.let { uid -> getUserData(uid) }
                // Case sign in failed
                } else {
                    handleException(task.exception, "Login Failed")
                    // Process done
                    inProgress.value = false
                }
            }

            // Inquiry signing in failed
            .addOnFailureListener { exc ->
                handleException(exc, "Login Failed")
                // Process done
                inProgress.value = false
            }
    }

    // private function creating or updating profile (called from other methods in ViewModel)
    private fun createOrUpdateProfile(
        name: String? = null,
        username: String? = null,
        bio: String? = null,
        imageUrl: String? = null
    ) {
        val uid = auth.currentUser?.uid
        val userData = UserData(
            userId = uid,
            name = name ?: userData.value?.name,
            username = username ?: userData.value?.username,
            bio = bio ?: userData.value?.bio,
            imageUrl = imageUrl ?: userData.value?.imageUrl,
            following = userData.value?.following
        )

        uid?.let { uid ->
            inProgress.value = true
            db.collection(USERS).document(uid).get()

                .addOnSuccessListener {
                    if (it.exists()) {
                        it.reference.update(userData.toMap())
                            .addOnSuccessListener {
                                this.userData.value = userData
                                inProgress.value = false
                            }
                            .addOnFailureListener { exc ->
                                handleException(exc, "Cannot update user")
                                inProgress.value = false
                            }
                    } else {
                        db.collection(USERS).document(uid).set(userData)
                        getUserData(uid)
                    }
                }

                .addOnFailureListener { exc ->
                    handleException(exc, "Cannot create user")
                    inProgress.value = false
                }
        }
    }

    // Private function to retrieve userdata from Firestore Database
    private fun getUserData(uid: String) {
        // Process begin
        inProgress.value = true
        // Retrieve user data from USERS collection on Firestore Database using UID
        db.collection(USERS).document(uid).get()

            .addOnSuccessListener {
                val user = it.toObject<UserData>()
                userData.value = user
                inProgress.value = false
                refreshPosts()
                getPersonalizedFeed()
                getFollowers(uid)
            }
            // Handle failure to retrieving data from Firestore Database
            .addOnFailureListener { exc ->
                handleException(exc, "Cannot retrieve user data")
                inProgress.value = false
            }
    }

    fun handleException(exception: Exception? = null, customMessage: String = "") {
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isEmpty()) errorMsg else "$customMessage: $errorMsg"
        popupNotification.value = Event(message)
    }

    private fun uploadImage(uri: Uri, onSuccess: (Uri) -> Unit) {
        inProgress.value = true

        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("images/$uuid")
        val uploadTask = imageRef.putFile(uri)

        uploadTask
            .addOnSuccessListener {
                val result = it.metadata?.reference?.downloadUrl
                result?.addOnSuccessListener(onSuccess)
            }

            .addOnFailureListener { exc ->
                handleException(exc)
                inProgress.value = false
            }
    }

    fun updateProfileData(name: String, username: String, bio: String) {
        createOrUpdateProfile(name, username, bio)
    }

    fun uploadProfileImage(uri: Uri) {
        uploadImage(uri) {
            createOrUpdateProfile(imageUrl = it.toString())
        }
    }

    fun onLogout() {
        // Logout from Firbase authentication
        auth.signOut()

        //  Let view model knows logged out
        signedIn.value = false
        userData.value = null

        // Message notify logout
        popupNotification.value = Event("Logged out")

        // Clear all the data sets related to user logged out
        searchedPosts.value = listOf()
        postsFeed.value = listOf()
        comments.value = listOf()
    }

    fun onNewPost(uri: Uri, description: String, onPostSuccess: () -> Unit) {
        uploadImage(uri) {
            onCreatePost(uri, description, onPostSuccess)
        }
    }

    private fun onCreatePost(imageUri: Uri, description: String, onPostSuccess: () -> Unit) {

        inProgress.value = true

        val currentUid = auth.currentUser?.uid
        val currentUsername = userData.value?.username
        val currentUserImage = userData.value?.imageUrl

        if (currentUid != null) {

            val postUuid = UUID.randomUUID().toString()

            val fillerWords = listOf("the", "be", "to", "is", "of", "and", "or", "a", "in", "it")
            val searchTerms = description
                .split(" ", ".", ",", "?", "!", "#")
                .map { it.lowercase() }
                .filter { it.isNotEmpty() and !fillerWords.contains(it) }


            val post = PostData(
                postId = postUuid,
                userId = currentUid,
                username = currentUsername,
                userImage = currentUserImage,
                postImage = imageUri.toString(),
                postDescription = description,
                time = System.currentTimeMillis(),
                likes = listOf<String>(),
                searchTerms = searchTerms
            )

            db.collection(POSTS).document(postUuid).set(post)
                .addOnSuccessListener {
                    popupNotification.value = Event("Post successfully created")
                    inProgress.value = false
                    refreshPosts()
                    onPostSuccess.invoke()
                }
                .addOnFailureListener { exc ->
                    handleException(exc, "Unable to create post")
                    inProgress.value = false
                }

        } else {
            handleException(customMessage = "Error: username unavailable, Unable to create post")
            onLogout()
            inProgress.value = false
        }
    }

    private fun refreshPosts() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            refreshPostsProgress.value = true
            db.collection(POSTS).whereEqualTo("userId", currentUid).get()
                .addOnSuccessListener { documents ->
                    convertPosts(documents, posts)
                    refreshPostsProgress.value = false
                }
                .addOnFailureListener { exc ->
                    handleException(exc, "Cannot fetch posts")
                    refreshPostsProgress.value = false
                }
        } else {
            handleException(customMessage = "Error: username unavailable. Unable to refresh posts")
            onLogout()
        }
    }

    private fun convertPosts(documents: QuerySnapshot, outState: MutableState<List<PostData>>) {

        val newPosts = mutableListOf<PostData>()

        documents.forEach { doc ->
            val post = doc.toObject<PostData>()
            newPosts.add(post)
        }
        val sortedPosts = newPosts.sortedByDescending { it.time }
        outState.value = sortedPosts
    }

    fun searchPosts(searchTerm: String) {
        if (searchTerm.isNotEmpty()) {
            searchedPostsProgress.value = true
            db.collection(POSTS).whereArrayContains("searchTerms", searchTerm.trim().lowercase())
                .get()
                .addOnSuccessListener {
                    convertPosts(it, searchedPosts)
                    searchedPostsProgress.value = false
                }
                .addOnFailureListener { exc ->
                    handleException(exc, "Cannot search posts")
                    searchedPostsProgress.value = false
                }
        }
    }

    fun onFollowClick(userId: String) {
        auth.currentUser?.uid?.let { currentUser ->
            val following = arrayListOf<String>()
            userData.value?.following?.let {
                following.addAll(it)
            }
            if (following.contains(userId)) {
                following.remove(userId)
            } else {
                following.add(userId)
            }
            db.collection(USERS).document(currentUser).update("following", following)
                .addOnSuccessListener {
                    getUserData(currentUser)
                }
        }
    }

    private fun getPersonalizedFeed() {
        val following = userData.value?.following

        // Case user has any followings
        if (!following.isNullOrEmpty()) {

            postsFeedProgress.value = true

            // Get "POSTS" collections with userId in following list
            db.collection(POSTS).whereIn("usderId", following).get()
                .addOnSuccessListener {
                    convertPosts(documents = it, outState = postsFeed)
                    // Case no posts with followings
                    if (postsFeed.value.isEmpty()) {
                        getGeneralFeed()
                    } else {
                        postsFeedProgress.value = false
                    }
                }
                .addOnFailureListener { exc ->
                    handleException(exc, "Cannot get personlized feed")
                    postsFeedProgress.value = false
                }
            //  Case user has no followings
        } else {
            getGeneralFeed()
        }
    }

    // Function which load general feeds
    private fun getGeneralFeed() {
        postsFeedProgress.value = true
        val currentTime = System.currentTimeMillis()
        val difference = 24 * 60 * 60 * 1000 // 1day in millis
        // Get POSTS collections from Firebase store in the range of 24hrs
        db.collection(POSTS)
            .whereGreaterThan("time", currentTime - difference)
            .get()
            .addOnSuccessListener {
                convertPosts(documents = it, outState = postsFeed)
                postsFeedProgress.value = false
            }
            .addOnFailureListener { exc ->
                handleException(exc, "Cannot get feed")
                postsFeedProgress.value = false
            }
    }

    fun onLikePost(postData: PostData) {
        auth.currentUser?.uid?.let { userId ->
            postData.likes?.let { likes ->
                val newLikes = arrayListOf<String>()
                if (likes.contains(userId)) {
                    newLikes.addAll(likes.filter { userId != it })
                } else {
                    newLikes.addAll(likes)
                    newLikes.add(userId)
                }
                postData.postId?.let { postId ->
                    db.collection(POSTS).document(postId).update("lieks", newLikes)
                        .addOnSuccessListener {
                            postData.likes = newLikes
                        }
                        .addOnFailureListener { exc ->
                            handleException(exc, "Unable to like post")
                        }
                }
            }
        }
    }

    fun createComment(postId: String, text: String) {
        userData.value?.username?.let { username ->
            val commentId = UUID.randomUUID().toString()
            val comment = CommentData(
                commentId = commentId,
                postId = postId,
                username = username,
                text = text,
                timestap = System.currentTimeMillis()
            )

            db.collection(COMMENTS).document(commentId).set(comment)
                .addOnSuccessListener {
                    // Get existing comments
                    getComments(postId)
                }
                .addOnFailureListener { exc ->
                    handleException(exc, "Cannot create comment.")
                }
        }
    }

    fun getComments(postId: String?) {
        // Update comment loading progress
        commentsProgress.value = true

        // Get comments for specific postId from "comments" collection in FirebaseStore
        db.collection(COMMENTS).whereEqualTo("postId", postId).get()
            .addOnSuccessListener { documents ->
                val newComments = mutableListOf<CommentData>()

                // Get each comment to convert into type CommentData
                documents.forEach { doc ->
                    val comment = doc.toObject<CommentData>()
                    // Add each comment in newComments of mutable list
                    newComments.add(comment)
                }
                // Sort all the comments by time descending
                val sortedComments = newComments.sortedByDescending { it.timestap }
                commentsProgress.value = false
            }
            .addOnFailureListener { exc ->
                // Handles exception of failure of retrieving comments from FirebaseStore
                handleException(exc, "Cannot retrieve comments")
                commentsProgress.value = false
            }
    }

    private fun getFollowers(uid: String) {
        db.collection(USERS).whereArrayContains("following", uid ?: "").get()
            .addOnSuccessListener { documents ->
                followers.value = documents.size()
            }
    }
}

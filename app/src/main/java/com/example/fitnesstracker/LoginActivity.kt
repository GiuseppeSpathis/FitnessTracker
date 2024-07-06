package com.example.fitnesstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.core.updateTransition
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton: Button
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var goToRegistration: TextView
    private lateinit var togglePasswordVisibilityButton: ImageButton
    private val socialModel = SocialModel()
    private val REQUEST_PERMISSION_REQUEST_CODE = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_registration2)
        notificationRequest()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                val userData = socialModel.fetchUserData(currentUser.uid)
                if (userData != null) {
                    LoggedUser.username = userData.username ?: ""
                    LoggedUser.gender = userData.gender ?: ""
                    LoggedUser.email = userData.email ?: ""
                    LoggedUser.id = userData.id

                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    Log.e("LoginActivity", "Error while saving user data")
                    showToast(R.string.errore_login)
                }
            }
            return
        }

        loginButton = findViewById(R.id.login)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        goToRegistration = findViewById(R.id.register)
        togglePasswordVisibilityButton = findViewById(R.id.togglePasswordVisibilityButton)

        togglePasswordVisibilityButton.setOnClickListener {
            if (password.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordVisibilityButton.setImageResource(R.drawable.visible)
            } else {
                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordVisibilityButton.setImageResource(R.drawable.not_visible)
            }
            password.setSelection(password.text.length)
        }

        goToRegistration.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

        loginButton.setOnClickListener {
            lifecycleScope.launch {
                val insertedEmail = email.text.toString()
                val insertedPassword = password.text.toString()
                signInAndFetchUserData(insertedEmail, insertedPassword)
            }
        }
    }



    private suspend fun signInAndFetchUserData(insertedEmail: String, insertedPassword: String) {
        try {
            val authResult = withContext(Dispatchers.IO) {
                auth.signInWithEmailAndPassword(insertedEmail, insertedPassword).await()
            }

            updateUI(authResult.user)

            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            val userData = socialModel.fetchUserData(uid)

            if (userData != null) {
                LoggedUser.username = userData.username ?: ""
                LoggedUser.gender = userData.gender ?: ""
                LoggedUser.email = userData.email ?: ""
                LoggedUser.id = uid
            } else {
                Log.e("LoginActivity", "Error while saving user data")
            }

        } catch (e: Exception) {
            handleSignInError(e)
        }
    }

    private suspend fun handleSignInError(e: Exception) {
        when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> showToast(R.string.email_errata)
                    "ERROR_WRONG_PASSWORD" -> showToast(R.string.password_errata)
                    else -> showToast(R.string.wrong_credentials)
                }
            }
            is FirebaseAuthInvalidUserException -> {
                when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> showToast(R.string.user_not_found)
                    else -> showToast(R.string.not_valid_user)
                }
            }
            else -> {
                showToast(R.string.errore_login)
            }
        }
        Log.e("LoginActivity", "Error during sign-in", e)
    }

    private fun showToast(message: Int) {

            Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()


    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            showToast(R.string.successo_login)

            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }, 2000)
        } else {
            showToast(R.string.errore_login)
        }
    }
    private fun notificationRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_PERMISSION_REQUEST_CODE)
        }
    }
}


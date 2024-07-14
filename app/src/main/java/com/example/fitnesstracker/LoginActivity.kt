package com.example.fitnesstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton: Button
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var goToRegistration: TextView
    private lateinit var togglePasswordVisibilityButton: ImageButton
    private val model = Model()
    private val REQUEST_PERMISSION_REQUEST_CODE = 1
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_registration2)
        notificationRequest()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                val userData = model.fetchUserData(currentUser.uid)
                if (userData != null) {
                    LoggedUser.username = userData.username
                    LoggedUser.gender = userData.gender
                    LoggedUser.email = userData.email
                    LoggedUser.id = userData.id

                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    Log.e("LoginActivity", "Error while saving user data")
                    MotionToast.createColorToast(
                        this@LoginActivity,
                        this@LoginActivity.resources.getString(R.string.error),
                        this@LoginActivity.resources.getString(R.string.errore_login),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))
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
            val userData = model.fetchUserData(uid)

            if (userData != null) {
                LoggedUser.username = userData.username
                LoggedUser.gender = userData.gender
                LoggedUser.email = userData.email
                LoggedUser.id = uid
            } else {
                Log.e("LoginActivity", "Error while saving user data")
            }

        } catch (e: Exception) {
            handleSignInError(e)
        }
    }

    private fun handleSignInError(e: Exception) {
        when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" ->
                    MotionToast.createColorToast(
                        this@LoginActivity,
                        this@LoginActivity.resources.getString(R.string.error),
                        this@LoginActivity.resources.getString(R.string.email_errata),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))

                    "ERROR_WRONG_PASSWORD" ->
                    MotionToast.createColorToast(
                        this@LoginActivity,
                        this@LoginActivity.resources.getString(R.string.error),
                        this@LoginActivity.resources.getString(R.string.password_errata),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))

                    else ->
                    MotionToast.createColorToast(
                        this@LoginActivity,
                        this@LoginActivity.resources.getString(R.string.error),
                        this@LoginActivity.resources.getString(R.string.wrong_credentials),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))

                }
            }
            is FirebaseAuthInvalidUserException -> {
                when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" ->
                    MotionToast.createColorToast(
                        this@LoginActivity,
                        this@LoginActivity.resources.getString(R.string.error),
                        this@LoginActivity.resources.getString(R.string.user_not_found),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))

                    else ->
                    MotionToast.createColorToast(
                        this@LoginActivity,
                        this@LoginActivity.resources.getString(R.string.error),
                        this@LoginActivity.resources.getString(R.string.not_valid_user),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))

                }
            }
            else -> {
                MotionToast.createColorToast(
                    this@LoginActivity,
                    this@LoginActivity.resources.getString(R.string.error),
                    this@LoginActivity.resources.getString(R.string.errore_login),
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))

            }
        }
        Log.e("LoginActivity", "Error during sign-in", e)
    }


    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            MotionToast.createColorToast(
                this@LoginActivity,
                this@LoginActivity.resources.getString(R.string.successo),
                this@LoginActivity.resources.getString(R.string.successo_login),
                MotionToastStyle.SUCCESS,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))


            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }, 2000)
        } else {
            MotionToast.createColorToast(
                this@LoginActivity,
                this@LoginActivity.resources.getString(R.string.error),
                this@LoginActivity.resources.getString(R.string.errore_login),
                MotionToastStyle.ERROR,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(this@LoginActivity, www.sanju.motiontoast.R.font.helvetica_regular))
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun notificationRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_PERMISSION_REQUEST_CODE)
        }
    }
}


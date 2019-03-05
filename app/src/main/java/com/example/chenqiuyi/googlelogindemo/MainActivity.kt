package com.example.chenqiuyi.googlelogindemo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import java.io.InputStreamReader


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var mTvIdToken: TextView? = null
    private var mTvAccessToken: TextView? = null
    private var mVSignOut: View? = null

    private val TAG = MainActivity::class.java.simpleName
    private val RC_GET_TOKEN = 9002
    private var mGoogleSignInClient: GoogleSignInClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        initView()
    }

    private fun init() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestServerAuthCode(getString(R.string.server_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initView() {
        mTvIdToken = findViewById(R.id.tv_id_token)
        mTvAccessToken = findViewById(R.id.tv_access_token)
        mVSignOut = findViewById(R.id.v_sign_out)
        findViewById<View>(R.id.bt_sign_in).setOnClickListener(this)
        findViewById<View>(R.id.v_sign_out).setOnClickListener(this)
        mVSignOut?.visibility = View.GONE
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.bt_sign_in -> signIn()
            R.id.v_sign_out -> signOut()
        }
    }

    private fun signIn() {
        val signIntent = mGoogleSignInClient?.signInIntent
        startActivityForResult(signIntent, RC_GET_TOKEN)
    }

    private fun signOut() {
        mTvIdToken?.text = ""
        mTvAccessToken?.text = ""
        mVSignOut?.visibility = View.GONE
        mGoogleSignInClient?.signOut()
    }

    private fun getIdToken(task: Task<GoogleSignInAccount>) {
        val account = task.getResult(ApiException::class.java)
        val idToken = account.idToken
        mTvIdToken?.text = idToken
        mVSignOut?.visibility = View.VISIBLE

        getAccessToken(account)
    }

    private fun getAccessToken(account: GoogleSignInAccount) {
        Thread {
            val authCode = account.serverAuthCode

            if (TextUtils.isEmpty(authCode)) {
                return@Thread
            }
            // Exchange auth code for access token
            val clientSecrets = GoogleClientSecrets.load(
                    JacksonFactory.getDefaultInstance(), InputStreamReader(assets.open("client_secret.json")))
            val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                    NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    "https://www.googleapis.com/oauth2/v4/token",
                    clientSecrets.details.clientId,
                    clientSecrets.details.clientSecret,
                    authCode,
                    "")  // Specify the same redirect URI that you use with your web
                    // app. If you don't have a web version of your app, you can
                    // specify an empty string.
                    .execute()

            val accessToken = tokenResponse.accessToken
            runOnUiThread {
                mTvAccessToken?.text = accessToken
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GET_TOKEN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                getIdToken(task)
            } catch (e: ApiException) {
                Log.w(TAG, "handleSignInResult:error", e)
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signOut()
    }
}

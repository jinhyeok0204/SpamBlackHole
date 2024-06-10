package com.example.spamblackhole2

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.spamblackhole2.databinding.ActivityEmailDetailBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailDetailActivity : AppCompatActivity() {

    private val binding by lazy { ActivityEmailDetailBinding.inflate(layoutInflater) }
    private lateinit var credential: GoogleAccountCredential
    private lateinit var emailId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        emailId = intent.getStringExtra("email_id") ?: ""

        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.let{
            credential = GoogleAccountCredential.usingOAuth2(
                this, listOf("https://www.googleapis.com/auth/gmail.readonly")
            )
            credential.selectedAccount = android.accounts.Account(it.email, "com.google")

            fetchEmailDetails()
        }

        binding.backButton.setOnClickListener{
            onBackPressed()
        }
    }

    private fun fetchEmailDetails() {
        val gmailService = Gmail.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("Gmail Reader")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try{
                val message = gmailService.users().messages().get("me", emailId).setFormat("full").execute()
                val headers = message.payload.headers
                val subjectHeader = headers.find{it.name=="Subject"}
                val subject = subjectHeader?.value ?: "No Subject"
                val body = getBodyFromMessage(message)

                withContext(Dispatchers.Main){
                    binding.emailSubject.text = subject
                    binding.emailBody.text = body
                }
            } catch(e: Exception){
                Log.e("EmailDetailActivity", "Error fetching email details", e)
            }
        }
    }

    private fun getBodyFromMessage(message:com.google.api.services.gmail.model.Message): String {
        val parts = message.payload.parts
        var body = ""

        if(parts!=null){
            for(part in parts){
                if(part.mimeType == "text/plain" || part.mimeType == "text/html"){
                    val data = part.body.data
                    if(data != null){
                        body = String(Base64.decode(data, Base64.URL_SAFE))
                        break
                    }
                }
            }
        } else{
            val data = message.payload.body.data
            if(data != null){
                body = String(Base64.decode(data, Base64.URL_SAFE))
            }
        }

        return body
    }
}